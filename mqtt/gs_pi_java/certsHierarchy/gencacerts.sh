#!/usr/bin/env bash

############################# Optional Arguments ######################
#
# keystore_pwd     arg1: Optional. Password for trust store and key store. Taken from gspi.properties file if missing here.
# truststore_pwd   arg2: Optional. Password for trust store and key store. Taken from gspi.properties file if missing here.
# domain_name      arg3: Optional. Needs to be unique within all of gs. Change it to your domain name or something unique (your email address, a guid, ...). 
#                                  Can be reused for all of your devices. Taken from gspi.properties file if missing here.
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
keystore_pwd=${1:-$(prop 'KEYSTOREPWD')}
truststore_pwd=${2:-$(prop 'TRUSTSTOREPWD')} 
domain_name=${3:-$(prop 'DEVICE_DOMAIN')}

if [[ -z $keystore_pwd ]]
then
	keystore_pwd=$"keystore_password_CHANGE_THIS" 
fi
if [[ -z $truststore_pwd ]]
then
	truststore_pwd=$"truststore_password_CHANGE_THIS" 
fi 

echo "-----------------------"
echo "  keystore_pwd=${keystore_pwd}"
echo "  truststore_pwd=${truststore_pwd}"
echo "  domain_name=${domain_name}"

cakeystore_name="ca-keystore.p12"
echo "  cakeystore_name=${cakeystore_name}"

rm *.crt
rm *.p12
rm *.csr
rm "$cakeystore_name"

echo "-----------------------------------------------------------------"
echo "                        ca-root"
echo "                     /             \\"
echo "                   /                  \\"
echo "           ca-level1a                ca-level1b "
echo "           /      \\                 /        \\"
echo "     ca-level2a   ca-level2b   ca-level2c    ca-level2d"
echo "      /      \\                                /      \\"
echo " device1_2a   device2_2a               device3_2d   device4_2d "
echo "-----------------------------------------------------------------"


#---Root

echo "Root- Generating root keypair..."
keytool -storetype pkcs12 -keystore "$cakeystore_name" -storepass "$keystore_pwd" -keypass "$keystore_pwd" -alias "ca-root" -genkeypair -keyalg "RSA" -keysize 2048 -dname "CN=Root CA - ${domain_name}"  -validity 36000 -ext KeyUsage=digitalSignature,keyCertSign -ext bc=ca:true,PathLen:4

echo "Root- Exporting ca-root.crt ..."
keytool -storetype pkcs12 -keystore "$cakeystore_name" -storepass "$keystore_pwd" -alias "ca-root" -exportcert -rfc > "ca-root.crt"

#---Level 1

echo "Level1a - Generating ca-level1a keypair..."
keytool -storetype pkcs12 -keystore "$cakeystore_name" -storepass "$keystore_pwd" -keypass "$keystore_pwd" -alias "ca-level1a" -genkeypair -keyalg "RSA" -keysize 2048 -dname "CN=Level1a CA - ${domain_name}"  -validity 36000 -ext KeyUsage=digitalSignature,keyCertSign -ext bc=ca:true,PathLen:3

echo "Level1a - Exporting ca-level1a.csr (certificate signing request)..."
keytool -keystore "$cakeystore_name" -storepass "$keystore_pwd" -alias "ca-level1a" -certreq -file "ca-level1a.csr"

echo "Level1a - Using ca-root to sign ca-level1a..."
keytool -keystore  "$cakeystore_name" -storepass "$keystore_pwd" -alias "ca-root" -gencert -rfc -infile "ca-level1a.csr" -outfile "ca-level1a.crt" -validity 36000 -ext KeyUsage=digitalSignature,keyCertSign -ext bc=ca:true,PathLen:3

echo "Level1b - Generating ca-level1b keypair..."
keytool -storetype pkcs12 -keystore "$cakeystore_name" -storepass "$keystore_pwd" -keypass "$keystore_pwd" -alias "ca-level1b" -genkeypair -keyalg "RSA" -keysize 2048 -dname "CN=Level1b CA - ${domain_name}"  -validity 36000 -ext KeyUsage=digitalSignature,keyCertSign -ext bc=ca:true,PathLen:3

echo "Level1b - Exporting ca-level1b.csr (certificate signing request)..."
keytool -keystore "$cakeystore_name" -storepass "$keystore_pwd" -alias "ca-level1b" -certreq -file "ca-level1b.csr"

echo "Level1a - Using ca-root to sign ca-level1a..."
keytool -keystore  "$cakeystore_name" -storepass "$keystore_pwd" -alias "ca-root" -gencert -rfc -infile "ca-level1b.csr" -outfile "ca-level1b.crt" -validity 36000 -ext KeyUsage=digitalSignature,keyCertSign -ext bc=ca:true,PathLen:3

#---Level 2 a,b

echo "Level2a - Generating ca-level2a keypair..."
keytool -storetype pkcs12 -keystore "$cakeystore_name" -storepass "$keystore_pwd" -keypass "$keystore_pwd" -alias "ca-level2a" -genkeypair -keyalg "RSA" -keysize 2048 -dname "CN=Level2a CA - ${domain_name}"  -validity 36000 -ext KeyUsage=digitalSignature,keyCertSign -ext bc=ca:true,PathLen:3

echo "Level2a - Exporting ca-level2a.csr (certificate signing request)..."
keytool -keystore "$cakeystore_name" -storepass "$keystore_pwd" -alias "ca-level2a" -certreq -file "ca-level2a.csr"

echo "Level2a - Using ca-level1a to sign ca-level2a..."
keytool -keystore  "$cakeystore_name" -storepass "$keystore_pwd" -alias "ca-level1a" -gencert -rfc -infile "ca-level2a.csr" -outfile "ca-level2a.crt" -validity 36000 -ext KeyUsage=digitalSignature,keyCertSign -ext bc=ca:true,PathLen:3

echo "Level2b - Generating ca-level2b keypair..."
keytool -storetype pkcs12 -keystore "$cakeystore_name" -storepass "$keystore_pwd" -keypass "$keystore_pwd" -alias "ca-level2b" -genkeypair -keyalg "RSA" -keysize 2048 -dname "CN=Level2b CA - ${domain_name}"  -validity 36000 -ext KeyUsage=digitalSignature,keyCertSign -ext bc=ca:true,PathLen:3

echo "Level2b - Exporting ca-level2b.csr (certificate signing request)..."
keytool -keystore "$cakeystore_name" -storepass "$keystore_pwd" -alias "ca-level2b" -certreq -file "ca-level2b.csr"

echo "Level2a - Using ca-level1a to sign ca-level2b..."
keytool -keystore  "$cakeystore_name" -storepass "$keystore_pwd" -alias "ca-level1a" -gencert -rfc -infile "ca-level2b.csr" -outfile "ca-level2b.crt" -validity 36000 -ext KeyUsage=digitalSignature,keyCertSign -ext bc=ca:true,PathLen:3


#---Level 2 c,d

echo "Level2c - Generating ca-level2c keypair..."
keytool -storetype pkcs12 -keystore "$cakeystore_name" -storepass "$keystore_pwd" -keypass "$keystore_pwd" -alias "ca-level2c" -genkeypair -keyalg "RSA" -keysize 2048 -dname "CN=Level2c CA - ${domain_name}"  -validity 36000 -ext KeyUsage=digitalSignature,keyCertSign -ext bc=ca:true,PathLen:3

echo "Level2c - Exporting ca-level2c.csr (certificate signing request)..."
keytool -keystore "$cakeystore_name" -storepass "$keystore_pwd" -alias "ca-level2c" -certreq -file "ca-level2c.csr"

echo "Level2c - Using ca-level1b to sign ca-level2c..."
keytool -keystore  "$cakeystore_name" -storepass "$keystore_pwd" -alias "ca-level1b" -gencert -rfc -infile "ca-level2c.csr" -outfile "ca-level2c.crt" -validity 36000 -ext KeyUsage=digitalSignature,keyCertSign -ext bc=ca:true,PathLen:3

echo "Level2d - Generating ca-level2d keypair..."
keytool -storetype pkcs12 -keystore "$cakeystore_name" -storepass "$keystore_pwd" -keypass "$keystore_pwd" -alias "ca-level2d" -genkeypair -keyalg "RSA" -keysize 2048 -dname "CN=Level2d CA - ${domain_name}"  -validity 36000 -ext KeyUsage=digitalSignature,keyCertSign -ext bc=ca:true,PathLen:3

echo "Level2d - Exporting ca-level2d.csr (certificate signing request)..."
keytool -keystore "$cakeystore_name" -storepass "$keystore_pwd" -alias "ca-level2d" -certreq -file "ca-level2d.csr"

echo "Level2d - Using ca-level1b to sign ca-level2d..."
keytool -keystore  "$cakeystore_name" -storepass "$keystore_pwd" -alias "ca-level1b" -gencert -rfc -infile "ca-level2d.csr" -outfile "ca-level2d.crt" -validity 36000 -ext KeyUsage=digitalSignature,keyCertSign -ext bc=ca:true,PathLen:3

#---Generate crt Bundles

echo "Creating public certificate bundles..."
cat ca-level2a.crt ca-level1a.crt ca-root.crt > ca-level2a-bundle.crt
cat ca-level2b.crt ca-level1a.crt ca-root.crt > ca-level2b-bundle.crt
cat ca-level2c.crt ca-level1b.crt ca-root.crt > ca-level2c-bundle.crt
cat ca-level2d.crt ca-level1b.crt ca-root.crt > ca-level2d-bundle.crt

echo "Done"

#some diagnostic calls:

#keytool -list -keystore keystore.p12 -storepass keystore_password_CHANGE_THIS 
#$keytool -list -keystore truststore.p12 -storepass keystore_password_CHANGE_THIS 
#keytool -list -v -keystore keystore1.p12 -storepass keystore_password_CHANGE_THIS  | grep -A 1 "Owner"
#keytool -list -v -keystore truststore.p12 -storepass keystore_password_CHANGE_THIS | grep -A 1 "Alias"

#openssl x509 -in *.crt -text -noout

