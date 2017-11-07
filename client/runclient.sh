#!/bin/bash

javac -d bin -sourcepath src -cp lib/gson-2.6.2.jar src/Client.java

# If the program doesn't compile
if [ $? -ne 0 ]; then
    echo -e "\e[31mErrors, pls Reverify Code\e[0m"
fi

java -cp ./bin:./lib/gson-2.6.2.jar Client 43
