#!/usr/bin/env bash

############################# Optional Arguments ######################
#
# cert_ver	       arg1: Optional. Certificate version. Assists with cert rolling
# keystore_pwd     arg2: Optional. Password for trust store and key store
# truststore_pwd   arg3: Optional. Password for trust store and key store

# device_id      arg4: Optional. Needs to be unique within your gs organization
# device_name    arg5: Optional. Needs to be unique within the folder it will be created into within your gs organization
# domain_name    arg6: Optional. Needs to be unique within all of gs. Change it to your domain name or something unique (your email address, a guid, ...). 
#                                Can be reused for all of your devices.
# ca_crt_level2  arg7: Optional. The name of the certificate authority used to sign the device cert. Must be in the same directory. 
#                                  Assumes ca_crt_level2 is the alias name in the ca keyfile and file ca_crt_level2.crt exists in the same directory
# ca_crt_level1  arg8: Optional. The name of the certificate authority used to sign the device cert. Must be in the same directory. 
#                                  Assumes ca_crt_level1 is the alias name in the ca keyfile and file ca_crt_level2.crt exists in the same directory
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
ca_crt_level2=${7:-"ca-level2a"}
ca_crt_level1=${8:-"ca-level1a"}

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

device_dir="./$device_name"
keystore_name="$device_dir/keystore${cert_ver}.p12"
cakeystore_name="ca-keystore.p12"
ca_crt_file=$ca_crt_level2".crt"


echo "  keystore_name=${keystore_name}"
echo "  cakeystore_name=${cakeystore_name}"
echo "  ca_crt_level1=${ca_crt_level1}"
echo "  ca_crt_level2=${ca_crt_level2}"
echo "  ca_crt_file=${ca_crt_file}"
echo "  ca_crt_bundle=${ca_crt_bundle}"
echo "-----------------------"


mkdir -p $device_dir
rm -R "$device_dir/*"



#---------------

echo "Creating device keystore: ${keystore_name}..."
keytool -keystore "$keystore_name" -storepass "$keystore_pwd" -keypass "$keystore_pwd" -alias $device_id -genkeypair -keyalg "RSA" -keysize 2048 -dname "CN=${device_name} ${domain_name}, O=version_${cert_ver}" -validity 36000 -ext bc=ca:false -ext eku=sa,ca -ext san=dns:localhost,ip:127.0.0.1

echo "Generating device csr: ${device_name}${cert_ver}.csr"
keytool -keystore "$keystore_name" -storepass "$keystore_pwd" -alias $device_id -certreq -file "$device_dir/${device_name}${cert_ver}.csr"

echo "Using ${cakeystore_name} to create signed ${device_name}${cert_ver}.crt"
keytool -keystore  "$cakeystore_name" -storepass "$keystore_pwd" -alias $ca_crt_level2 -gencert -rfc -infile "$device_dir/${device_name}${cert_ver}.csr" -outfile "$device_dir/${device_name}${cert_ver}.crt" -validity 36000 -ext bc=ca:false -ext san=dns:localhost,ip:127.0.0.1

echo  "Importing the chain of CA certificates into keystore $device_dir/$keystore_name before replacing the unsigned server certificate with the signed one. Oracle/sun requires it this way to establish chain from reply"
keytool -keystore "$keystore_name" -storepass "$keystore_pwd" -keypass "$keystore_pwd" -importcert -alias "ca-root" -file "ca-root.crt" -noprompt
keytool -keystore "$keystore_name" -storepass "$keystore_pwd" -keypass "$keystore_pwd" -importcert -alias $ca_crt_level1 -file $ca_crt_level1".crt" -noprompt
keytool -keystore "$keystore_name" -storepass "$keystore_pwd" -keypass "$keystore_pwd" -importcert -alias $ca_crt_level2 -file $ca_crt_level2".crt" -noprompt

echo "Adding the signed ${device_name}${cert_ver}.crt to $keystore_name"
keytool -keystore "$keystore_name" -storepass "$keystore_pwd" -keypass "$keystore_pwd" -importcert -alias "$device_id" -file "$device_dir/${device_name}${cert_ver}.crt" -noprompt

echo "Deleting $ca_crt_level2  from keystore $keystore_name"
keytool -keystore "$keystore_name" -storepass "$keystore_pwd" -keypass "$keystore_pwd" -delete -alias "ca-root" -noprompt
keytool -keystore "$keystore_name" -storepass "$keystore_pwd" -keypass "$keystore_pwd" -delete -alias "ca-level1a" -noprompt
keytool -keystore "$keystore_name" -storepass "$keystore_pwd" -keypass "$keystore_pwd" -delete -alias $ca_crt_level2 -noprompt

echo "Adding gs_root.crt to truststore.p12..." 
keytool -storetype pkcs12 -keystore "$device_dir/truststore.p12" -storepass "$truststore_pwd" -keypass "$truststore_pwd" -importcert -alias gd-root -file /home/gs/Dev/certs/grovestreams.com/gd_root.crt -noprompt

echo "Done" 

#some diagnostic calls:


#$keytool -list -keystore truststore.p12 -storepass truststore_pwd 
#keytool -list -v -keystore truststore.p12 -storepass truststore_pwd -keypass truststore_pwd | grep -A 1 "Alias"
#keytool -list -v -keystore ca-keystore.p12 -storepass keystore_password_CHANGE_THIS -keypass keystore_password_CHANGE_THIS | grep -A 1 "Alias"

#openssl x509 -in *.crt -text -noout

