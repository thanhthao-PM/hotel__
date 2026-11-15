cd src\main\java
chcp 65001
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
[Console]::InputEncoding = [System.Text.Encoding]::UTF8
java "-Dfile.encoding=UTF-8" "-Dstdout.encoding=UTF-8" "-Dstdin.encoding=UTF-8" -cp ..\..\..\out hotel.Main