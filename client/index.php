<!DOCTYPE html>
<html>
 <head>
   <meta charset="UTF-8">
   <link rel="preconnect" href="https://fonts.gstatic.com">
   <link href="https://fonts.googleapis.com/css2?family=Open+Sans&display=swap" rel="stylesheet">
   <link href="https://fonts.googleapis.com/css2?family=Roboto+Mono:ital,wght@1,200&display=swap" rel="stylesheet">
  <style>
body{
  width: 600px;
  margin-left: 20px;
  font-family: "Open Sans";
  font-size: 18px;
}
.gray {
	color: #808080;
}
.monospace {
  font-family: 'Roboto Mono', monospace;
}
.smallcaps {
  font-variant: small-caps;
}
.italics {
  font-style: italic;
}
h2.originalword {
  font-size: 24px;
  margin-bottom: 3px;
}
a:link { color: black;}
a:visited { color: black;}
a:hover { color: black;}
a:active { color: black;}
.gray a:link { color: #808080;}
.gray a:visited { color: #808080;}
.gray a:hover { color: #808080;}
.gray a:active { color: #808080;}
div#resultats p {
line-height: 180%;
margin:0 0 0.2em 0;
}

.src_sent {
  font-size: .8em;
  color: #808080;
  font-weight: 750;
}
.tgt_sent {
  font-size: .8em;
  color: #808080;
}

</style>
</head>
<body id="cos">

<h2>Diccionari anglès-català</h2>

<form id="searchFrom" action="" onsubmit="sendSearchWord(); return false;" acceptcharset="UTF-8">
  <input type="text" id="cerca" name="cerca" placeholder="Escriviu una paraula en anglès o en català" autofocus><br><br>
  <input type="submit" value="Cerca"/>
  <input type="button" onclick="removeText();" value="Esborra"/>
</form>

<div id="resultats"></div>


<script type="text/javascript">
var api = "http://localhost:8084/search/";
var root = "http://localhost/engcat/index.php/"
var path = location.pathname;
var pathparts = path.split("/")
var word = decodeURI(pathparts[pathparts.length - 1]).replace(/[^0-9\/a-zA-ZàáèéìíòóùúïüÀÁÈÉÌÍÙÚÒÓÜÏçÇñÑ·\s\-\'’\.]/gi, '');
if (word.length > 0) {
  //document.forms["searchFrom"]["cerca"].value = word;	
  sendSearchWord(word);
}

function sendSearchWord(word) {
  if (word) {
    var cerca = word.trim();
    document.getElementById("cerca").value=cerca;
  } else {
    var cerca = document.getElementById("cerca").value.trim();
    window.location.href = root+cerca;
    return;
  }
  if (!cerca) {
    return;
  }
  var url = api + cerca;
  const req = new XMLHttpRequest();
  req.open('GET', url);
  req.responseType = 'json';
  req.onload = function(e) {
    var html = "";
    if (this.status == 200 || this.status == 404) {
      //console.log('response', this.response); // JSON response
      var obj = this.response;
      html = "<h3>Resultats per a «" + obj.searchedWord + "»</h3>";
      if (obj.results.length == 0) {
        html += "No s'han trobat resultats. ";
      } else {
        document.getElementById("cerca").select();
        document.getElementById("cerca").focus();
      }

      /*if (obj.alternatives.length > 0) {
        html += "Volíeu dir ";
        first = true;
        for (var word of obj.alternatives) {
          if (!first) {
            html += ", ";
          } else {
            first = false;
          }
          html += "<a href=\"" + root + word + "\">" + word + "</a>";
        }
        html += "?<br/>";
      }*/

      i = 0;
      for (var result of obj.results) {
        if (result.lemmas.length>0) {
          if (i==0) {
            html += "<h3>anglès → català</h3>";
          } else {
            html += "<h3>català → anglès</h3>";
          }
          for (var lemma of result.lemmas) {
            html += '<b>' + prepareLemmaHeading(lemma.originalWord) + "</b>";
            html += '<hr/>'
            for (var subLemma of lemma.subLemmaList) {
              html += "<ol>"
              preparedSublLemma = prepareSubLemma(subLemma.originalWord)
              if (preparedSublLemma.length > 0) {
                html += '<b>' + preparedSublLemma + "</b>&nbsp;";  
              }
              html += "<li>"
              fullGTag = fullGrammarTag(subLemma.originalWord);
              isFirstAfterLemma = true
              for (var translationsSet of subLemma.translationsSets) {
                isFirst = true;
                for (var translatedWord of translationsSet.translatedWords) {
                  if (isFirst) {
                    isFirst = false;
                    if (!isFirstAfterLemma) {
                      html += '</li><li>';
                    } else {
                      isFirstAfterLemma = false;
                    }
                  } else {
                    html +="&nbsp;| ";
                  }
                  html += prepareWord(translatedWord, fullGTag);
                }
                for (var example of translationsSet.examples) {
                  html += '<br/>&nbsp;&nbsp;<span class="src_sent">'+example.sourceSentence+'</span><span class="tgt_sent">&nbsp;&mdash;&nbsp;'+example.targetSentence+'</span>'
                }
              }
              html += '</li></ol>';
            }
            <!---->
          }
        }
        i++;
      }
      html = html.replaceAll('\'', '’')
      //html += JSON.stringify(obj);
    } else {
      html+="<p>ERROR: El servidor no respon.</p>";
    }
   document.getElementById("resultats").innerHTML = html;
  };
  req.send();
}

function fullGrammarTag(word) {
  var grammarTag = word.grammarClass;
  if (word.feminine && grammarTag=="m") {
    grammarTag="mf"
  }
  if (word.grammarAux) {
    grammarTag = grammarTag + "&nbsp;" + word.grammarAux;
  }
  return grammarTag;
}

function prepareLemmaHeading(word) {
  var output = "";
  output += '<h2 class="originalword">';
  output += word.text;
  if (word.feminine) {
    output += ' <span class="gray">'+word.feminine+'</span> ';
  }
  var fullGTag = fullGrammarTag(word)
  output +='&nbsp;<span class="italics">' + fullGTag + '</span>&nbsp;';
  output += '</h2>';
  if (word.plural) {
    output += ' [pl. '+word.plural+'] ';
  }
  if (word.tags) {
    output += '['+word.tags+'] ';
  }
  if (word.def) {
    output += '['+word.def+'] ';
  }
  if (word.remark) {
    output += ' [&rArr; '+word.remark+'] ';
  }
  return output.trim();
}

function prepareSubLemma(word) {
  var output = "";
  if (word.before || word.after) {
    output += '<b>';
    if (word.before) {
      output += '('+word.before+') ';
    }
    output += word.text;
    //if (word.feminine) {
    //  output += ' <span class="gray">'+word.feminine+'</span> ';
    //}
    if (word.after) {
      output += ' ('+word.after+')';
    }
    output += '</b>&nbsp;';
  }
  //var fullGTag = fullGrammarTag(word)
  // output +='<span class="italics">' + fullGTag + '</span>&nbsp;';
  if (word.area) {
    output += '<span class="smallcaps">'+word.area+'</span>&nbsp;';
  }
  /*if (word.plural) {
    output += ' [pl. '+word.plural+'] ';
  }*/
  /*if (word.tags) {
    output += '['+word.tags+'] ';
  }*/
  if (word.def) {
    output += '['+word.def+'] ';
  }
  /*if (word.remark) {
    output += ' [&rArr; '+word.remark+'] ';
  }*/
  return output.trim();
}

function prepareWord(word, prevFullGTag) {
  var output = "";
  if (word.area) {
    output += '<span class="smallcaps">'+word.area+'</span>&nbsp;';
  }
  if (word.tags) {
    output += '['+word.tags+'] ';
  }
  if (word.def) {
    output += '['+word.def+'] ';
  }
  if (word.before) {
    output += '('+word.before+') ';
  }
  output += word.text;
  if (word.after) {
    output += ' ('+word.after+')';
  }
  if (word.feminine) {
    output += '&nbsp;<span class="gray">'+word.feminine+'</span>';
  }
  if (word.plural) {
    output += ' [pl. '+word.plural+'] ';
  }
  var fullGTag = fullGrammarTag(word)
  if (fullGTag != prevFullGTag && fullGTag != "n") {
   output +='&nbsp;<span class="italics">' + fullGTag + '</span>';
  }
  if (word.remark) {
    output += ' [&rArr; '+word.remark+'] ';
  }
  return output.trim();
}

function presentFeminine(word) {
  if (word.feminineForm) {
    return "&nbsp;<span class=gray>" + word.feminineForm +"</span>";
  } else {
    return "";
  }
}

function removeText() {
  document.getElementById("cerca").value="";
  document.getElementById("cerca").focus();
}

</script>

</body>
</html>