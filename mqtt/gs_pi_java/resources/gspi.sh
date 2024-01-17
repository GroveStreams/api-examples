#!/usr/bin/env bash

 # **********************************************************************
 # ORGANIZATION  :  Grove Streams, LLC
 # PROJECT       :  GsPi :: An example of using GroveStreams.com on a Raspberry Pi
 # FILENAME      :  gspi.sh
 #
 # This file is part of the GsPi project. More information about
 # this project can be found here:  https://grovestreams.com/
 # **********************************************************************
 #
 # Licensed under the Apache License, Version 2.0 (the "License");
 # you may not use this file except in compliance with the License.
 # You may obtain a copy of the License at
 #
 #      http://www.apache.org/licenses/LICENSE-2.0
 #
 # Unless required by applicable law or agreed to in writing, software
 # distributed under the License is distributed on an "AS IS" BASIS,
 # WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 # See the License for the specific language governing permissions and
 # limitations under the License.
 
### BEGIN INIT INFO
# Provides:          Manages gspi
# Required-Start:    $all
# Required-Stop:
# Default-Start:     2 3 4 5
# Default-Stop:
# Short-Description: Manages gspi...
### END INIT INFO

export JAVA_HOME=/usr/lib/jvm/java-11-openjdk-arm64


scriptFile=$(readlink -fn $(type -p $0))                   # the absolute, dereferenced path of this script file
scriptDir=$(dirname $scriptFile)                           # absolute path of the script directory
applDir="$scriptDir"                                       # home directory of the service application
serviceName="gspi"                               	   # service name
serviceNameLo="gspi"                                    # service name with the first letter in lowercase
serviceUser="gs"                                          # OS user name for the service
serviceUserHome="$applDir"                                 # home directory of the service user
serviceGroup="gs"                                          # OS group name for the service
serviceLogFile="$applDir/logs/$serviceNameLo.sh.log"               # log file for StdOut/StdErr
maxShutdownTime=60                                         # maximum number of seconds to wait for the daemon to terminate normally
pidFile="/var/run/$serviceNameLo.pid"                      # name of PID file (PID = process ID number)
javaCommand="java"                                         # name of the Java launcher without the path
javaExe="$JAVA_HOME/bin/$javaCommand"                      # file name of the Java application launcher executable
javaArgs="-Xmx100m -cp $applDir/:$applDir/gspi.jar com.grovestreams.gspi.PiMain"   # arguments for Java launcher
javaCommandLine="$javaExe $javaArgs"                       # command line to start the Java service application
javaCommandLineKeyword="gspi"                    # a keyword that occurs on the commandline, used to detect an already running service process and to distinguish it from others
rcFileBaseName="rc$serviceNameLo"                          # basename of the "rc" symlink file for this script
rcFileName="/usr/local/sbin/$rcFileBaseName"               # full path of the "rc" symlink file for this script
etcInitDFile="/etc/init.d/$serviceNameLo"                  # symlink to this script from /etc/init.d

# Makes the file $1 writable by the group $serviceGroup.
function makeFileWritable {
   local filename="$1"
   touch $filename || return 1
   chgrp $serviceGroup $filename || return 1
   chmod g+w $filename || return 1
   return 0; }

# Returns 0 if the process with PID $1 is running.
function checkProcessIsRunning {
   local pid="$1"
   if [ -z "$pid" -o "$pid" == " " ]; then return 1; fi
   if [ ! -e /proc/$pid ]; then return 1; fi
   return 0; }

# Returns 0 if the process with PID $1 is our Java service process.
function checkProcessIsOurService {
   local pid="$1"
   local cmd="$(ps -p $pid --no-headers -o comm)"
   if [ "$cmd" != "$javaCommand" -a "$cmd" != "$javaCommand.bin" ]; then return 1; fi
   grep -q --binary -F "$javaCommandLineKeyword" /proc/$pid/cmdline
   if [ $? -ne 0 ]; then return 1; fi
   return 0; }

# Returns 0 when the service is running and sets the variable $servicePid to the PID.
function getServicePid {
   if [ ! -f $pidFile ]; then return 1; fi
   servicePid="$(<$pidFile)"
   checkProcessIsRunning $servicePid || return 1
   checkProcessIsOurService $servicePid || return 1
   return 0; }

function startServiceProcess {
   cd $applDir || return 1
   rm -f $pidFile
   makeFileWritable $pidFile || return 1
   makeFileWritable $serviceLogFile || return 1
   local cmd="setsid $javaCommandLine >>/dev/null 2>&1 & echo \$! >$pidFile"
   sudo $SHELL -c "$cmd" || return 1
   sleep 0.1
   servicePid="$(<$pidFile)"
   if checkProcessIsRunning $servicePid; then :; else
      echo "$serviceName start failed, see logfile."
      return 1
      fi
   return 0; }

function stopServiceProcess {
   kill $servicePid || return 1
   for ((i=0; i<maxShutdownTime*10; i++)); do
      checkProcessIsRunning $servicePid
      if [ $? -ne 0 ]; then
         rm -f $pidFile
         return 0
         fi
      sleep 0.1
      done
   echo -e "\n$serviceName did not terminate within $maxShutdownTime seconds, sending SIGKILL..."
   kill -s KILL $servicePid || return 1
   local killWaitTime=15
   for ((i=0; i<killWaitTime*10; i++)); do
      checkProcessIsRunning $servicePid
      if [ $? -ne 0 ]; then
         rm -f $pidFile
         return 0
         fi
      sleep 0.1
      done
   echo "Error: $serviceName could not be stopped within $maxShutdownTime+$killWaitTime seconds!"
   return 1; }

function runInConsoleMode {
   getServicePid
   if [ $? -eq 0 ]; then echo "$serviceName is already running"; return 1; fi
   cd $applDir || return 1
   sudo $javaCommandLine  || return 1
   if [ $? -eq 0 ]; then return 1; fi
   return 0; }

function startService {
   getServicePid
   if [ $? -eq 0 ]; then echo "$serviceName is already running";  return 0; fi
   echo "Starting $serviceName  "
   startServiceProcess
   if [ $? -ne 0 ]; then return 1; fi
   return 0; }

function stopService {
   getServicePid
   if [ $? -ne 0 ]; then echo "$serviceName is not running"; return 0; fi
   echo "Stopping $serviceName   "
   stopServiceProcess
   if [ $? -ne 0 ]; then return 1; fi

   return 0; }

function installService {
   
   sudo cat > "/etc/systemd/system/$serviceName.service" << EOF
[Unit]
Description=$serviceName Service
After=network.target

[Service]
User=root
Type=forking
RemainAfterExit=yes
WorkingDirectory=$applDir/
ExecStart=/bin/bash $applDir/$serviceName.sh start
ExecStop=/bin/bash $applDir/$serviceName.sh stop

[Install]
WantedBy=multi-user.target
EOF
    echo "Reloading daemon, enabling and starting service..."
    sudo systemctl daemon-reload 
    sudo systemctl enable "$serviceName.service" # remove the extension
    sudo systemctl start "$serviceName.service"
    echo "Service Started"
    
   echo $serviceName installed.
   echo Service has started. Service registered for auto-start during bootup. You may now use:
   echo "  sudo systemctl start $serviceName.service" 
   echo "  sudo systemctl stop $serviceName.service" 
   echo "  sudo systemctl status $serviceName.service" 
   return 0; }

function uninstallService {
   echo "Stopping and disabling service..."
   sudo systemctl stop $serviceName.service
   sudo systemctl disable $serviceName.service
   echo "Service Disabled. Uninstalling..."
   
   rm /etc/systemd/system/$serviceName.service
   
   systemctl daemon-reload
   systemctl reset-failed

   echo "$serviceName.service uninstalled."
   return 0; }

function main {
  
   case "$1" in
      console)                                             # runs the Java program in console mode
         runInConsoleMode
         ;;
      start)                                               # starts the Java program as a Linux service
         startService
         ;;
      stop)                                                # stops the Java program service
         stopService
         ;;
      restart)                                             # stops and restarts the service
         stopService && startService
         ;;
      install)                                             # installs the service in the OS
         installService
         ;;
      uninstall)                                           # uninstalls the service in the OS
         uninstallService
         ;;
      *)
         echo "Usage: $0 {console|start|stop|restart|install|uninstall}"
         exit 1
         ;;
      esac
    }

main $1

