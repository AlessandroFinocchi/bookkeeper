# Execute the shell code when ba-dua profile is activated

input_file="badua.xml"
modified_file="badua_pretty.xml"
cookie_class="org/apache/bookkeeper/bookie/Cookie"
booking_state_manager_class="org/apache/bookkeeper/bookie/BookieStateManager"

xmllint --format target/$input_file > target/$modified_file

rm target/$input_file
cp "target/$modified_file" "target/badua_Cookie.xml"
cp "target/$modified_file" "target/badua_BookieStateManager.xml"

xmlstarlet ed -L -d \
 "//class[not(contains(@name, '$cookie_class'))]" \
 "target/badua_Cookie.xml"

xmlstarlet ed -L -d \
 "//class[not(contains(@name, '$booking_state_manager_class'))]" \
 "target/badua_BookieStateManager.xml"