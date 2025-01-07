# Execute the shell code when ba-dua profile is activated

input_file="badua.xml"
modified_file="badua_pretty.xml"
buffered_channel_class="org/apache/bookkeeper/bookie/BufferedChannel"

xmllint --format target/$input_file > target/$modified_file

rm target/$input_file
cp "target/$modified_file" "target/badua_BufferedChannel.xml"

xmlstarlet ed -L -d \
 "//class[not(contains(@name, '$buffered_channel_class'))]" \
 "target/badua_BufferedChannel.xml"