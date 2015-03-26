


import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;

import loci.common.Log4jTools;
import loci.common.LogbackTools;
import loci.formats.*;
import ini.trakem2.*;
import ini.trakem2.display.*;
import ini.trakem2.persistence.*;


public class FixTrakEM2Io {
	
	public static void test( String[] args ) { 
		
//		String ipfn = "/Users/bogovicj/Documents/projects/alignment/MB-Z1213-56-align/badimgexamples/Merlin-4238_15-01-22_075823_2-0-0_InLens.tif";
		String ipfn = "/Users/bogovicj/Documents/projects/alignment/MB-Z1213-56-align/badimgexamples/Merlin-4238_15-02-05_095204_1-0-0_InLens.tif";
		
		//String fn = "/Users/bogovicj/Documents/projects/alignment/MB-Z1213-56-align/badimgexamples/import.txt";
		String fn = "/Users/bogovicj/workspaces/alignment/parallel-elastic-alignment/0-99/import.txt";
		
		String tem2dir = "/Users/bogovicj/tmp/trakem2_tmp";
		ControlWindow.setGUIEnabled(false);
		Project project = Project.newFSProject( "blank", null, tem2dir, false );
		
		
	    Loader loader = project.getLoader();
	    LayerSet layerset = project.getRootLayerSet();
	    layerset.setSnapshotsMode(1);
	    
//	    ImagePlus ip = loader.openImagePlus( ipfn );
//	    System.out.println("ip: " + ip );
	    
	    
	        
//	    /* add a reference layer (pointless gymnastik) */
//	    Layer layer = new Layer(project, 0, 1, layerset);
//	    layerset.add(layer);
//	    layer.recreateBuckets();
//	    
//	    Bureaucrat task = loader.importImages(
//	            layerset.getLayer(0),   // the first layer
//	            fn,                   // the absolute file path to the text file with absolute image file paths
//	            " ",                    // the column separator  <path> <x> <y> <section index>
//	            1.0,                    // section thickness, defaults to 1
//	            1.0,                    // calibration, defaults to 1
//	            false,                  // whether to homogenize contrast, avoid
//	            1.0f,                   // scaling factor, default to 1
//	            0);                     // border width
//	                
//	    /* wait until all images have been imported */
//	    try {
//			task.join();
//		} catch (InterruptedException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
		
		// destroy and exit
//		p.destroy();
//		System.exit(0);
		
	}

	public static void main(String[] args) {
		
		LogbackTools.enableLogging("OFF");
		
		String input = args[0];
		String[] fileList = null;
		
		if( input.endsWith( "tif" )){
			fileList = args;
		}else{
			ArrayList<String> lines = new ArrayList<String>();
			try {
				BufferedReader br = new BufferedReader(new FileReader(args[0]));
			    String line;
			    while ((line = br.readLine()) != null) {
			    	lines.add( line );
			    }
			    br.close();
			    
			    fileList = new String[ lines.size() ];
			    lines.toArray(fileList);
			}catch( Exception e ){
				e.printStackTrace();
				System.exit(1);
			}
		}
		
	    ChannelSeparator fr = new ChannelSeparator();
		fr.setGroupFiles(false);
		
		for( int i=0; i<fileList.length; i++ ){
			
			String ipfn = fileList[i];
			//System.out.println("working on " + ipfn );
			
			boolean iserror = false;
			try {
				fr.setId(ipfn);
			}catch( Exception e ){
				//e.printStackTrace();
				iserror = true;
			}
			try{
				fr.close();
			}catch( Exception e ){
			}
		
			if( iserror )
				System.out.println("" + ipfn );

		}

	}

}
