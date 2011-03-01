/*
 * Function: error
 * Description: prints an error message and quits
 * Parameters: char* msg  -> the error message to print
 * Returns: nothing
 */
void error(char * msg)
{
  fprintf(stderr, "%s\n", msg);
  exit(1);
}

/*
 * Function: check_no_protocol
 * Description: checks that there is no ':' character in the URL because if
 * 				there is, then we have to deal with protocols, and we don't
 * 				want to do that yet.. this IS a beta version. For now, we
 * 				warnt the user and quit if we find a ':'
 * Parameters: char* url  -> the url to check.
 * Returns: nothing
 */
void check_no_protocol(char* url)
{
  while(*url)
  {
    if(*url == ':')
    {
      printf("Incorrect format; just use www.company.com PORT\n");
      exit(1);
    }
    url++;
  }
}

/*
 * Function: clean
 * Description: replace all instances of line feed and carriage return with
 * 				their benign representations.
 * Parameters: char* msg  -> the msg to clean.
 * Returns: a new, cleaned message (does not change the original message)
 */
char* clean(char* msg)
{
	// Our output will be at most twice the size of our input
	char* output = (char*) malloc(strlen(msg) * sizeof(char));

	// Loop through our input text and replace all instances of line feed and
	// carriage return to their benign representations.
	int original_index = 0;
	int output_index = 0;
	while(original_index < strlen(msg))
	{
		switch (msg[original_index])
		{
			case '\r':	output[output_index++] = '\\';
						output[output_index++] = 'r';
						break;

			case '\n':	output[output_index++] = '\\';
						output[output_index++] = 'n';
						break;

			default:	output[output_index++] = msg[original_index];
		}
		original_index++;
	}

	return output;
}

/*
 * Function: usage
 * Description: prints usage information
 * Parameters: char* argv[] -> the command line args passed in
 * Returns: nothing
 */
void usage(char* argv[])
{
	fprintf(stderr,"usage %s [options] hostname\n", argv[0]);
	fprintf(stderr,"  Options:\n");
	fprintf(stderr,"    -v Verbose\n");
	exit(0);
}

#define LARGEST_PAGE 2048 	// Largest supported web page size
