if [ $# -ne "1" ]
then
  echo Usage: driver.sh CONFIG_FILE
fi

LIBDIR=../libs
jars=`ls $LIBDIR/*.jar`
CP=""
for jar in $jars
do
  CP=$CP:$jar
done

export CLASSPATH=$CP
java -cp $CLASSPATH main/Driver $1
