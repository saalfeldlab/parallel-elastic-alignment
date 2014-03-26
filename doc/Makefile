all : header.txt manual.md
	cat header.txt > manual.html
	markdown manual.md >> manual.html
	cat footer.txt >> manual.html
clean :
	rm manual.html *~

