#!/usr/bin/env bash

############################# Optional Arguments ######################
#
# cert_ver	       arg1: Optional. Certificate version. Assists with cert rolling
# keystore_pwd     arg2: Optional. Password for key store. Taken from .properties file if missing here.
# truststore_pwd   arg3: Optional. Password for trust store. Taken from .properties file if missing here.

# device_id      arg4: Optional. Needs to be unique within your gs organization. Taken from .properties file if missing here.
# device_name    arg5: Optional. Needs to be unique within the folder it will be created into within your gs organization. Taken from .properties file if missing here.
# domain_name    arg6: Optional. Needs to be unique within all of gs. Change it to your domain name or something unique (your email address, a guid, ...). 
#                                Can be reused for all of your devices. Taken from .properties file if missing here.
#
#######################################################################

propsfile="../gspi.properties"
if ! test -f "${propsfile}" ; then
    propsfile="../resources/gspi.properties"
fi
echo "propsfile=${propsfile}"

function prop {
	#Read property from pi properties file. Ignore lines starting with #. Trim spaces (| xargs)
    grep "^[^#]" ${propsfile} | grep "^${1}" | cut -d'=' -f2 | xargs
}

#Assemble needed variables

cert_ver=${1:-"1"}
keystore_pwd=${2:-$(prop 'KEYSTOREPWD')}
truststore_pwd=${3:-$(prop 'TRUSTSTOREPWD')} 
device_id=${4:-$(prop 'DEVICE_ID')}
device_name=${5:-$(prop 'DEVICE_NAME')}
domain_name=${6:-$(prop 'DEVICE_DOMAIN')}

if [[ -z $keystore_pwd ]]
then
	keystore_pwd=$"keystore_password_CHANGE_THIS" 
fi
if [[ -z $truststore_pwd ]]
then
	truststore_pwd=$"truststore_password_CHANGE_THIS" 
fi 
if [[ -z $device_id ]]
then
	#Serial number
	device_id=$(cat /proc/cpuinfo | grep Serial | cut -d ' ' -f 2)
	if [[ -z $device_id ]]
	then
		#MAC address
		device_id=$(ifconfig | grep ether | cut -d ' ' -f10)
		if [[ -z $device_id ]]
		then
			device_id=$"gs_pi"
		fi
	fi
fi
if [[ -z $device_name ]]
then
	device_name="$HOSTNAME($device_id)"
fi 


echo "-----------------------"
echo "  cert_ver=${cert_ver}"
echo "  keystore_pwd=${keystore_pwd}"
echo "  truststore_pwd=${truststore_pwd}"
echo "  domain_name=${domain_name}"
echo "  device_id=${device_id}"
echo "  device_name=${device_name}"

keystore_name="keystore${cert_ver}.p12"
echo "  keystore_name=${keystore_name}"
echo "-----------------------"

rm "$keystore_name"
rm "truststore.p12"

echo "creating a keystore with gs private key and crt"
keytool -storetype pkcs12 -keystore "$keystore_name" -storepass "$keystore_pwd" -keypass "$keystore_pwd" -alias "${device_name}" -genkeypair -keyalg "RSA" -keysize 2048 -dname "CN=${device_name} ${domain_name} Self Signed, O=version_${cert_ver}" -validity 36000

echo "Exporting public certificate..."
keytool -storetype pkcs12 -keystore "$keystore_name" -storepass "$keystore_pwd" -alias "${device_name}" -exportcert -rfc > "${device_name}-${cert_ver}.crt"

echo "add gs_root.crt to truststore"
keytool -storetype pkcs12 -keystore "truststore.p12" -storepass "$truststore_pwd" -keypass "$truststore_pwd" -importcert -alias gd-root -file ./gd_root.crt -noprompt


#some diagnostic calls:

#keytool -list -keystore keystore.p12 -storepass keystore_password_CHANGE_THIS 
#$keytool -list -keystore truststore.p12 -storepass keystore_password_CHANGE_THIS 
#keytool -list -v -keystore keystore1.p12 -storepass keystore_password_CHANGE_THIS  | grep -A 1 "Owner"
#keytool -list -v -keystore truststore.p12 -storepass keystore_password_CHANGE_THIS | grep -A 1 "Alias"


#openssl x509 -in *.crt -text -noout


