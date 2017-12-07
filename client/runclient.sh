#!/bin/bash

javac -d bin -sourcepath src -cp lib/gson-2.6.2.jar src/Client.java

# If the program doesn't compile
if [ $? -ne 0 ]; then
    echo -e "\e[31mErrors, pls Reverify Code\e[0m"
    exit 1
fi

java -Djava.security.debug=sunpkcs11 -cp ./bin:./lib/gson-2.6.2.jar Client 43
