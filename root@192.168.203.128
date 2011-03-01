/*
 * CSE 484 Lab 3
 *  A simple (and possibly vulnerable) url fetcher
 *
 */


#include <netdb.h>
#include <netinet/in.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/types.h>
#include <sys/socket.h>
#include <unistd.h>
#include "cgethttp.h"

int main(int argc, char *argv[])
{
  // The main variables
  char buffer[LARGEST_PAGE];        // Buffer for data
  int sockfd;                       // The socket file descriptor
  int portno;						// The port number
  struct sockaddr_in serv_addr;     // Structure to hold our server address
  struct hostent *server;           // Structure to hold our server

  char verbose = 0;					// Flag that is set if the -v option is provided

  // Check usage
  if (argc < 2)
	usage(argv);

  // Check to see if the user supplied options
  short server_loc = 1;             // Location of the server string in the command args
  if( argc == 3 )
  {
	// Check to see if it was a verbose flag
	if( strcmp(argv[1],"-v") == 0 )
	{
	  verbose = 1;
	  server_loc++;
	}
	// Hmm, it was something else, let's print usage and die
	else
	  usage(argv);
  }

  // Check to make sure the user didn't supply a protocol
  check_no_protocol(argv[server_loc]);

  // Print buffer location
  if(verbose)
  {
    printf(" [  OK  ] Buffer at %x\n",buffer);
  }

  // Get port and socket file descriptor
  portno = 80;
  sockfd = socket(AF_INET, SOCK_STREAM, 0);
  if (sockfd < 0)
    error("ERROR opening socket");
  else if(verbose)
    fprintf(stderr," [  OK  ] opened socket fd\n");

  // Tokenize URL to make the file path we'll use later in the GET
  char* server_name = strtok(argv[server_loc],"/");
  char file[1024];
  memset(file,0,1024);
  char* temp;
  while ( (temp = strtok(NULL,"/")) != NULL )
  {
    file[strlen(file)] = '/';
    strcat(file,temp);
  }
  if(file[0] == 0)
    file[0] = '/';

  if(verbose)
  {
    fprintf(stderr, " [  OK  ] server_name: %s, file: %s\n",server_name,file);
  }

  // Get the server
  server = gethostbyname(server_name);
  if (server == NULL)
  {
    fprintf(stderr,"ERROR, no such host <%s>",argv[server_loc]);
    exit(0);
  }
  else if( verbose )
    fprintf(stderr," [  OK  ] got host by name\n");

  // Initialize server structure
  bzero((char *) &serv_addr, sizeof(serv_addr));
  serv_addr.sin_family = AF_INET;
  bcopy((char *)server->h_addr, (char *)&serv_addr.sin_addr.s_addr,
		  server->h_length);
  serv_addr.sin_port = htons(portno);

  // Connect to the server
  if (connect(sockfd, (const struct sockaddr*) &serv_addr,sizeof(serv_addr)) < 0)
    error("ERROR connecting");
  else if(verbose)
    fprintf(stderr," [  OK  ] successfully connected to server\n");


  // Create request string
  strcpy(buffer,"GET ");
  strcat(buffer,file);
  strcat(buffer, " HTTP/1.1\r\n");
  strcat(buffer,"User-Agent: Wget/1.10.2\r\n");
  strcat(buffer,"Host: ");
  strcat(buffer,server_name);
  strcat(buffer,"\r\n");
  strcat(buffer,"Connection: close\r\n\r\n");

  if(verbose)
  {
    fprintf(stderr, " [  OK  ] Sending: %s\n",clean(buffer));
  }

  // Send it out on the socket
  unsigned int num_bytes;	// Records the number of bytes read or sent
  num_bytes = write(sockfd,buffer,strlen(buffer));
  if (num_bytes < 0)
    error("ERROR writing to socket");
  else if(verbose)
    fprintf(stderr, " [  OK  ] making a GET request to server\n");

  // Get some data from server. We have to do this in a loop because MTU is
  // usually around 1500 bytes
  bzero(buffer,LARGEST_PAGE);
  while( (num_bytes = read(sockfd,buffer,LARGEST_PAGE)) > 0 )
  {
    if(verbose)
      fprintf(stderr, " [  OK  ] read %u bytes from server\n",num_bytes);

    // Print our results
    sprintf(buffer, LARGEST_PAGE);
    bzero(buffer,LARGEST_PAGE);

  }
  if (num_bytes < 0)
    error("ERROR reading from socket");LARGEST_PAGE ,

  return 0;
}
