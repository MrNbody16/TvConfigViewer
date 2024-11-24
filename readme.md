**Usage**


in order to fetch the subs or configs . start a python3 http server and serv a directory containing files like this : 

servdir / 
				--- Subs / 
								--- subs.txt
				--- Config / 
								--- configs.txt


you can serve the directory via this command : 
		
		 py -m http.server portnum --bind 0.0.0.0

then after getting your network ip from your network card adapter , check weather the directory can be access via your browser or not . if it is then you can install the app and enter your server ip:port in the following field.

then you have to tabs : 
	1- Subs 
	2- Configs 
if you click on each one the list will be fetched form server and will be shown in the screen. 

make sure each config or sub is in a line because configViewer reads them line by line . 
after that if you click on any item application copies the content into clipboard and opens nekobox . 
nekoBox has added the compatibility of android tvs and for now ConfigViewer will work with nekoBox. 

then you can use import from clipboard any where in the nekobox to import the config into it.
