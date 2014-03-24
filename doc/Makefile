all : header.txt manual.txt
	cat header.txt > manual.html
	markdown manual.txt >> manual.html
	cat footer.txt >> manual.html
clean :
	rm manual.html *~

