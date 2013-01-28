
outputType=".txt"
outputFile=$1$outputType
ant file > $outputFile
if grep -q "(A) wins" $outputFile
then
echo "Win against $2 on $1">>results.txt
else
echo "Loss against $2 on $1">>results.txt
fi
rm $outputFile
