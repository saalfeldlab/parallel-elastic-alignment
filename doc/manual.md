# Documentation

Elastic alignment of long FIB-SEM series.

We split elastic alignment of the entire series into parallel alignment jobs for chunks with fix size (e.g. 100) that are overlapping by 50% (e.g. 0-99, 50-149, 100-199, ...).  The independently aligned chunks are then aligned to each other with a rigid transformation that is calculated from a sample of corresponding locations across the overlapping ranges.  Finally, sections are exported with a transformation that is the linear interpolant of both independently estimated transformations in the overlapping range, weights run from 0-1 across the range.

## Preparation

0.  Make sure fiji is installed with an executable at:

        ~/packages/Fiji.app/fiji-linux64

1.	Create a list of all files to be imported with their absolute paths:

		find /groups/flyem/data/AL-Z0613-14/raw/ -name "*.tif" | sort > ls-sorted.txt

	For testing, extract a subset, e.g.:

		head -n 200 ls-sorted.txt > ls-sorted.0-199.txt		

2.	Identify an appropriate contrast range [*min*, *max*] for the images by manually inspecting them.  In fact, contrast can be estimated automatically, but that requires to open each individual image during import which is significant I/O overhead.  A good contrast range for alignment shows the texture to-be-aligned clearly while not being excessively saturated.  Open the image in *ImageJ*, open the *Adjust Brightness and Contrast* dialog and find appropriate values.  Test the values on other images in the series.

3. When many tif files are generated, some may not be readable by the due to header issues.  Validate and correct the files with 

        ./checkAndFixTifs list-sorted.txt    

    which will test all the filest in list-sorted.txt, if a file is unreadable it will attempt to create a readable copy in a new directory called 'fixed,' and a new text file, 'list-sorted-fixed', is created where any problematic files are replaced with their corresponding versions in the 'fixed' directory.

4.	Prepare a series of directories with TrakEM2 import.txt files for 50% overlapping chunks of specified size from the list, e.g.:

		./make-import-overlap ls-sorted.txt 100 16000 31000 1

	The name of the directory is the series range, e.g. '0-49', '0-99', '50-149', '100-199', ... The respective `import.txt` file specifies the *x*, *y*, *z* location of each image (*x*, *y* is 0, *z* is a running integer, its dimensions ('-' to let TrakEM2 figure it out), a preferred contrast range [*min*, *max*] and the image type (0: `uint8` (gray), 1: `uint16` (gray), 2: `float32` (gray), 3: `uint8` (indexed color), 4s: `uint32` (RGB color))

	The script
		
		./clean-import-overlap ls-sorted.txt 100

	removes the generated directories recursively.  Use it to clean up and start from scratch.

## Import and Align Chunks

Alignment requires that Fiji is installed in `${HOME}/packages/Fiji.app/`.  Please use the version from `/groups/jain/home/saalfelds/packages/Fiji.app` which is a custom build that has been tested.

1.	Modify the parameters in the script `import-and-align.bsh` to your needs.  Test alignment parameters on a representative chunk.  In an interactive graphical session, open Fiji, create a new TrakEM2 project and import the chunk.  First, test affine alignment parameters (`paramAffine`), although I do not expect much need for adjustment here.  Then align the series with those parameters.  Save the project.  Export a chunk of 5 sections around the worst spot of the affinely aligned series as an RGB stack with green (0, 255, 0) background.  Test blockmatching parameters as described at the [Fiji Wiki](http://fiji.sc/Test_Block_Matching_Parameters).  Then adjust the elastic alignment parameters (`paramElastic`) accordingly.

2.	Create parallel jobs for import and alignment:

		./create-jobs

	The script finds all directories in `./` that match `[digit]+-[digit]+` and creates a bash-script `./jobs/align-[digit]+-[digit]+` for each.

3.	Submit all jobs:

		./submit-jobs

	Check for projects with errors  
		
		./ranges-to-redo -h  

    the `-h` suffix indicates that the output will be printed in a human-readable format.  The script will output any rangest that failed for `non-trivial` reasons.  After addressing any potential problems, resubmit the remaining jobs with

		./submit-jobs-if-error

	The script
		
		./clean-trakem2-projects ls-sorted.txt 100

	removes the generated TrakEM2 projects and supporting files.  Use it to keep the `import.txt` files but otherwise start from scratch.

	The script
		
		./clean-trakem2-mipmaps ls-sorted.txt 100

	removes the generated TrakEM2 supporting files but keeps the `import.txt` and project files.  Use this to save disk space as the supporting files can be regenerated later from the project files.

## Test Success

The Janelia cluster has occasional file I/O hickups and previous opening and saving operations may have failed unnoticed.  In addition, a very low number of images seem to have malformed TIFF-tags that prevents them from being imported into TrakEM2 properly.  We therefore have to test all generated TrakEM2 projects, fix the identified issues and re-execute the corresponding alignment jobs.

The script
		./create-test-projects-job ls-sorted.txt 100

generates a test job that checks the existence of all images in all TrakEM2 projects in all directories in `./` that match `[digit]+-[digit]+`.  Submit the job with

		./submit-test-projects-job

and, after the job has finished, grep the output for missing images (there should be two projects affected by each bad image), e.g.

		cd jobs/log
		grep missing test-projects.o7027208
		  29673.0 missing
		  29673.0 missing
		  32918.0 missing
		  32918.0 missing
		  33993.0 missing
		  33993.0 missing
		cd ../..

Open the `import.txt` files in the corresponding directories and identify the offending image files.  If the files exist and are not broken, try to open and re-save them with ImageJ.  Re-execute the align jobs for the compromised ranges:

		


## Rigidly Align Series of Chunks<a name="rigid"></a>

We recommend [creating backup copies](#backup) of the `project.xml` files before running rigid alignment.

As series chunks overlap by 50%, each image is present in two independent chunks (except the first and last half chunks).  While we expect 'elastic' deformation to be averaged away, the relative orientation of two chunks is completely arbitrary.  Therefore, we align the series of chunks with a rigid transformation.  To that end, corresponding locations are sampled in the overlapping parts of two chunks, and then, for all chunks, a rigid transformation is estimated such that the sum of all square displacements is minimized.  The rigid transformations can be estimated pairwise and are accumulated later.

Create pairwise alignment jobs:

                ./create-align-overlapping-projects-pair-jobs ls-sorted.txt 100

Submit them:

		./submit-align-overlapping-projects-pair-jobs ls-sorted.txt 100

On our cluster, a low number of projects will not open correctly, triggering a Sax XML-Parser exception.  Reasons may be manyfold and today beyond my access.  Therefore, resubmit all jobs that haven't delivered a result:

		./submit-align-overlapping-projects-pair-jobs-if-not-exists ls-sorted.txt 100

Then, create the job to accumulate the transformations and update the joint bounding box for all ranges:

                ./create-apply-aligned-overlapping-projects-job ls-sorted.txt 100

Submit them (a):

		./submit-apply-aligned-overlapping-projects-job

After the job has finished, you can check the sanity of the resulting project files by calling (b):

		./sanity-check
                echo $?

If your files are sane, the first line will not produce any output and the second line prints 0. Otherwise, you will see the folders containing corrupt files (first line) and a printed 1 (second line) on stdout. In the latter case, go back to (a) and repeat until there are no corrupt files.


## Export Interpolated Chunks

We are now ready to export the entire series by interpolating the estimated transformations across chunks.

1.	Create export jobs

		./create-export-jobs ls-sorted.txt 100

2.	Submit export jobs

		./submit-export-jobs ls-sorted.txt 100

3.	Visually inspect results from a scaled and cropped export

		./scale-and-crop-export

	after you have modified the crop region in the second convert command.  Note that this script will generate and submit a large number of jobs (one per image) in short time which may cause trouble with the cluster setup.


## Other Stuff

### Backing up projects<a name="backup"></a>
 
The script

		./backup-projects

creates a backup copy of all `project.xml` files in all directories in `./` that match `[digit]+-[digit]+`.

Conversely, the script

		./restore-projects

copies the backup copy over the `project.xml` file in all directories in `./` that match `[digit]+-[digit]+`.  Use these scripts to test destructive parts of the pipeline.


### Re-running specific chunks 

Occasionally, a set of parameters will give rise to undesired behavior.  Re-running the elastic alignment for only the ranges with such behavior is desirable both to save unnecessary computation and headaches.  First, it is good practice to save a copy of `import-and-align.bsh` as a record of the parameters used to align the chunks that you will not be adjusting further.

Next, to help select parameters for the troublesome chunks, use the ["Extract Block Matching Correspondences"](http://fiji.sc/Elastic_Alignment_and_Montage) plugin in Fiji.  Once the parameters are selected, copy them into `import-and-align.bsh`, and then run the `align-<range>` scripts for the relevant chunks.  Visually inspect the results of these newly aligned chunks.


Next, reset the bounding boxes of the TrakEM2 projects using the script

		./create-run-resetBox-jobs

This step is necessary to ensure that the rigid alignment step to follow runs correctly.

Finally, re-run the [rigid alignment](#rigid) as described above.
