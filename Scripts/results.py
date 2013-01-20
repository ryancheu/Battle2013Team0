f = open('results.txt', 'a+')
r = open('stats.txt','w+')

import csv
wins = {}
loss = {}


lines = f.readlines()
if(len(lines)>0):
    name = lines[0].rsplit(' ',1)[1].rstrip('\n')
    winner = name +"wins.csv"
    loser =name+ "loss.csv"

    temp = open(winner,'a+')
    temp.close()
    temp = open(loser,'a+')
    temp.close()

    for key, val in csv.reader(open(winner)):
        wins[key] = val
    for key, val in csv.reader(open(loser)):
        loss[key] = val

    for i in range(1,len(lines)):
        line = lines[i].split()
        if(line[0]=='Win'):
            if(line[2] in wins.keys()):
                wins[line[2]]= int(wins[line[2]])+1
            else:
                wins[line[2]]=1
        else:
            if(line[2] in loss.keys()):
                loss[line[2]] = int(loss[line[2]])+1
            else:
                loss[line[2]]=1

    w = csv.writer(open(name+"wins.csv", "w"))
    for key, val in wins.items():
        w.writerow([key, val])
        print("Wins against " + key + ": " + str(val))
    x = csv.writer(open(name+"loss.csv", "w"))
    for key, val in loss.items():
        x.writerow([key, val])
        print("Losses against " + key + ": " + str(val))
    f.close()
