#include <iostream>
#include <vector>
#include <string>

using namespace std;

int main() {
  vector<string> theLines;
  string tempString;

  getline(cin,tempString);
  theLines.push_back(tempString);
  while ( true ) {
    getline(cin,tempString);
    if ( tempString.length() > 1) {
      theLines.push_back(tempString);
    }
    else {
      break;
    }
  }
  for ( int i = theLines.size()-1; i >= 0; i-- ) {
    for ( int j = theLines[i].length()-1; j >= 0; j-- ) {
      cout<<theLines[i][j];
    }
    cout << endl;
  }
  return 0; 
}
