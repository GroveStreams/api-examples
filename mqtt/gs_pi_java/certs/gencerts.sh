#!/usr/bin/env bash

############################# Optional Arguments ######################
#
# cert_ver	       arg1: Optional. Certificate version. Assists with cert rolling
# keystore_pwd     arg2: Optional. Password for trust store and key store
# truststore_pwd   arg3: Optional. Password for trust store and key store

# device_id      arg4: Optional. Needs to be unique within your gs organization
# device_name    arg5: Optional. Needs to be unique within the folder it will be created into within your gs organization
# domain_name    arg6: Optional. Needs to be unique within all of gs. Change it to your domain name or something unique (your email address, a guid, ...). Can be reused for all of your devices.
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
	device_id=$(cat /proc/cpuinfo | grep Serial | cut -d ' ' -f 2)
	if [[ -z $device_id ]]
	then
		device_id=$"gs_pi"
	
	fi 
fi 
if [[ -z $device_name ]]
then
	device_name=$HOSTNAME
fi 


echo "-----------------------"
echo "  cert_ver=${cert_ver}"
echo "  keystore_pwd=${keystore_pwd}"
echo "  truststore_pwd=${truststore_pwd}"
echo "  domain_name=${domain_name}"
echo "  device_id=${device_id}"
echo "  device_name=${device_name}"

keystore_name="keystore${cert_ver}.p12"
cakeystore_name="ca-keystore${cert_ver}.p12"
echo "  keystore_name=${keystore_name}"
echo "  cakeystore_name=${cakeystore_name}"
echo "-----------------------"

rm "$keystore_name"
rm "$cakeystore_name"
rm "truststore.p12"

#create a keystore with gs private key and crt - used to sign other keys
keytool -storetype pkcs12 -keystore "$cakeystore_name" -storepass "$keystore_pwd" -keypass "$keystore_pwd" -alias "${device_name}-ca" -genkeypair -keyalg "RSA" -keysize 2048 -dname "CN=${device_name} (${device_id}) ${domain_name} Certificate Authority, O=version_${cert_ver}" -validity 36000 -ext KeyUsage=digitalSignature,keyCertSign -ext bc=ca:true,PathLen:3
echo "1"

keytool -storetype pkcs12 -keystore "$cakeystore_name" -storepass "$keystore_pwd" -alias "${device_name}-ca" -exportcert -rfc > "${device_name}-ca${cert_ver}.crt"
echo "2"
#generate key pair

keytool -keystore "$keystore_name" -storepass "$keystore_pwd" -keypass "$keystore_pwd" -alias $device_id -genkeypair -keyalg "RSA" -keysize 2048 -dname "CN=${device_name} (${device_id}) ${domain_name}, O=version_${cert_ver}" -validity 36000 -ext bc=ca:false -ext eku=sa,ca -ext san=dns:localhost,ip:127.0.0.1
echo "3"
#generate csr ($device_id.csr)
keytool -keystore "$keystore_name" -storepass "$keystore_pwd" -alias $device_id -certreq -file "${device_name}${cert_ver}.csr"
echo "4"
#use csr to create signed $device_id.crt
keytool -keystore  "$cakeystore_name" -storepass "$keystore_pwd" -alias "${device_name}-ca" -gencert -rfc -infile "${device_name}${cert_ver}.csr" -outfile "${device_name}${cert_ver}.crt" -validity 36000 -ext bc=ca:false -ext san=dns:localhost,ip:127.0.0.1
echo "5"
#temporally import the CA certificate into the keystore before replacing the unsigned server certificate with the signed one. Oracle/sun just designed/required it this way
keytool -keystore "$keystore_name" -storepass "$keystore_pwd" -keypass "$keystore_pwd" -importcert -alias "${device_name}-ca" -file "${device_name}-ca${cert_ver}.crt" -noprompt
echo "6"
#add signed crt to keystore
keytool -keystore "$keystore_name" -storepass "$keystore_pwd" -keypass "$keystore_pwd" -importcert -alias "$device_id" -file "${device_name}${cert_ver}.crt" -noprompt
echo "7"
#delete ca crt from keystore
keytool -keystore "$keystore_name" -storepass "$keystore_pwd" -keypass "$keystore_pwd" -delete -alias "${device_name}-ca" -noprompt
echo "8"

#add gs_root.crt to truststore
keytool -storetype pkcs12 -keystore "truststore.p12" -storepass "$truststore_pwd" -keypass "$truststore_pwd" -importcert -alias gd-root -file ./gd_root.crt -noprompt


#some diagnostic calls:

#keytool -list -keystore keystore.p12 -storepass gs_pi_store_Pass 
#$keytool -list -keystore truststore.p12 -storepass truststore_pwd 
#keytool -list -v -keystore truststore.p12 -storepass truststore_pwd -keypass truststore_pwd | grep -A 1 "Alias"
#keytool -list -v -keystore keystore1.p12 -storepass gs_pi_store_Pass -keypass gs_pi_store_Pass | grep -A 1 "Alias"

#openssl x509 -in *.crt -text -noout


