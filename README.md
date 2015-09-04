##Build##
This is build using adb and Android Studio.


**Get the results file:**  
*Works on the Nexus.*  
Show export: ```adb shell cat /data/data/com.enrise.beacondatacollector/files/results.txt```   
Save results as local file: ```adb shell cat /data/data/com.enrise.beacondatacollector/files/results.txt >> file.txt```

## Data plotter
- Run npm install to install node_modules
- Map some local vagrant box to this folder so you can open the index in your browser (opening it as a file will prevent JS from loading)
