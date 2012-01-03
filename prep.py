import sqlite3
import os
import json
import shutil
from operator import itemgetter, attrgetter

def dict_factory(cursor, row):
    d = {}
    for idx, col in enumerate(cursor.description):
        d[col[0]] = row[idx]
    return d

def sorter(x,y):
	c = cmp(x["platform_level"],y["platform_level"]); 
	if(c==0): 
		return cmp(x["width"],y["width"]) 
	else: 
		return c
#shutil.rmtree("target",True)
platforms = "/Users/rgravener/android-sdk-macosx/platforms/"
#os.makedirs("target")
conn = sqlite3.connect("icons.db")
conn.row_factory = dict_factory
c = conn.cursor()
c.execute("select platform,name,drawable_level,sha,width,height,length from images")
names = {}
hashes = {}
levels = set();
for r in c.fetchall():
	name = r['name']
	r['platform_level'] = int(r['platform'].split("-")[1])
	n = names[name] if name in names else []
	names[name] = n
	n.append(r)
	shahash = r['sha']
	h = hashes[shahash] if shahash in hashes else []
	hashes[shahash] = h
	h.append(h)
	levels.add(r["drawable_level"])
levelAlias = {}
aliasLevel = {}
print levels
i = 1
for level in levels:
	levelAlias[level] = i
	aliasLevel[i] = level
	i = i+1
maxH=0
maxW=0
for name in names:
	if "ic_menu" not in name:
		continue
	names[name] = sorted(names[name],cmp = sorter)
	first = names[name][0]
	maxH = max(maxH,first["height"])
	maxW = max(maxW,first["width"])

print maxH,maxW
html = """

<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">

<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en" lang="en">
<head>
	<meta http-equiv="Content-Type" content="text/html; charset=utf-8"/>
	<title>:title</title>
	<script type="text/javascript" src="http://ajax.googleapis.com/ajax/libs/jquery/1.7.1/jquery.min.js"></script>
	<style type="text/css" media="screen">
			#controls {
				width: 150px;
				float: left;
			}
			#available img {
				background-image:url('checker.png');
			}
			#grid-container {
				float: right;
				left: 150px;
				top: 0;
				position: fixed
			}
			ul#grid li {
				list-style: none outside;
				float: left;
				margin-right: 20px;
				margin-bottom: 20px;
				width: :widthpx;
				height: :heightpx;
				text-align: center;
			}
				ul#grid li img {
					vertical-align: middle;
				}
				ul#grid li.d {
					background: black;
				}
			textarea {
				width:150px;
				height:50px;
			}
			body {
				font-size: 0.6em;
				font-type: Arial;
			}
	</style>
	
	<script type="text/javascript">
	var versions = {
		"3":"Cupcake 1.5",
		"4":"1.6 Donut",
		"7":"2.1 Eclair",
		"8":"2.2 Froyo",
		"9":"2.3.1 Gingerbread",
		"10":"2.3.2 Gingerbread",
		"11":"Honeycomb 3.1",
		"12":"Honeycomb 3.2",
		"13":"Honeycomb 3.3",
		"14":"4.0 Ice Cream Sandwich",
		"15":"4.0 Ice Cream Sandwich"
	};
	var drawables = :drawables;
	var data = :data;
	</script>
	
	<script type="text/javascript">
	$(document).ready(function() {
		$("#grid li").css("cursor","pointer");
		$("#controls").hide();
		$("#grid li").click(function() {						
			$("#controls").hide();
			name = $(this).find("img").attr('alt');
			rname = $(this).find("img").attr('title');
			$("#rname").text(rname);
			$("#name").val(name);
			$("#available").empty();
			var types = [drawables["drawable-hdpi"],drawables["drawable-mdpi"],drawables["drawable-ldpi"],drawables["drawable"]];
			var types2 =["drawable-hdpi","drawable-mdpi","drawable-ldpi","drawable"];
			for(platform in data[rname]) {
				$("#available").append(versions[platform]+"<br/>");
				var toUse = null;
				for(var k in types) {
					if(toUse!=null) {
						break;
					}
					var s = null;
					info = data[rname];
					extra = info[platform];					
					for (var i in extra) {
						level = extra[i]
						console.log("level",level,"type",types[k],"type2",types2[k]);
						if(level==types[k]) {
							toUse = types2[k]
							break;
						}
					}
				}
				var img = "android-"+platform+"/data/res/"+toUse+"/"+rname+".png";
				$("#available").append("<img src='"+img+"'/><br/>");
			}
			$("#controls").fadeIn();
		});
	});
	</script>
</head>

<body>

	<div id="container">
		<div id="controls">
			<span id="rname">Name</span>
			<textarea type="text" id="name"></textarea>
			available for\n
			<div id="available"></div>
		</div>
		<div id="grid-container">
			<ul id="grid">
				:grid
			</ul>
		</div>
	</div>
</body>
</html>
"""
grid = ""
more = {}
for name in names:
	if "ic_menu" not in name:
		continue
	images = names[name]
	image = images[0]
	li = "\t\t<li"
	if "dark" in name:
		li = li + " class=\"d\">"
	else:
		li = li +">"
	extra = ""
	if image["height"] < maxH:
		extra = " style=\"padding-top: " + str((maxH-image["height"])/2) +"px;\""
	li = li+"<img alt=\"android.R.drawable." + image["name"] + "\" title=\"" + image["name"] + "\" src=\""+image["platform"]+"/data/res/"+image["drawable_level"]+"/"+name+".png\" width=\""+str(image["width"])+"\" height=\""+str(image["height"]) +"\""+extra+"></li>\n"
	grid = grid+li
	more[name] = {}
	for image in images:
		plats = None
		platform = image["platform_level"]
		folder = image["platform"]+"/data/res/"+image["drawable_level"]+"/"
		back = folder+image["name"]+".png"
#		if not os.path.exists("target/"+folder):
#			os.makedirs("target/"+folder) 
#		shutil.copyfile(platforms+back,"target/"+back)
		if platform in more[name]:
			plats = more[name][platform]
		else:
			plats = []
			more[name][platform] = plats
		plats.append(levelAlias[image["drawable_level"]])	
f = open("target/names.html","wb")
f.write(html.replace(":width",str(maxW)).replace(":height",str(maxH)).replace(":title","Android Menu Item Icons").replace(":grid",grid).replace(":drawables",json.dumps(levelAlias)).replace(":data",json.dumps(more)))
f.close()

	