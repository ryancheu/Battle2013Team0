
import glob
import csv
import sys

for arg in sys.argv:
    if arg=='scores.py':
        pass
    elif arg == 'delete':
        for filename in glob.glob('*.csv'):
            f=open(filename,'w+')
            f.close()
    elif arg is not None:
        print(arg)
        name = arg
        winner = name +"wins.csv"
        loser =name+ "loss.csv"
        temp = open(winner,'a+')
        temp.close()
        temp = open(loser,'a+')
        temp.close()
        for key, val in csv.reader(open(winner)):
            print("Wins against " + key + ": " + val)
        for key, val in csv.reader(open(loser)):
            print("Losses against " + key + ": " + val)

