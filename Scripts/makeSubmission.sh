#!/bin/bash

team=BaseBot

cd ../..
rm -rf teams/team116
cp -r teams/$team teams/team116
temp=`mktemp`
for file in `find teams/team116|grep java`
do
	sed -r "s/$team/team116/g" $file > $temp
	mv $temp $file
done

ant -Dteam=team116 jar
mv submission.jar $team.jar
