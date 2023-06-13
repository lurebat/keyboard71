var jormrpc;
var nintype_loggedin = false;

var initiatejormrpc = function(){
    jormrpc = new JormRPC("", {
    
        local:{
            message: function(title, note){
                humane.log([title, note]);
            },
            fillexport: function(title, payload){
               $("#consoleexporttitle").text(title);
               $("#consoleexportbox").val(payload);
               karbase.activate("consoleexport");
            },
            takeimport: function(callback){
               callback("hereyougo", $('#consoleimportbox').val());
            },
            gotowexport: function(){
            },
            autologin : function(){
               humane.log("autologged in!");
               karbase.activate('introkarl');
            }
        },
        onopen: function(){
        },
        onbecomeconnected: function(){
          humane.log("Got connected!");
          $("#statusbar").html("connected");
          nintype_loggedin = false;
          karbase.activate('loginkarl');
          
        },
        onbecomedisconnected: function(){
          $("#statusbar").html("not connected");
        }
    
    });
};

var performLogin = function(){
   var thepass = $('#loginpassword').val();
   
   jormrpc.invoke('login', thepass, function(result){
      if(result == "done"){
         humane.log("logged in!");
         karbase.activate('introkarl');
      } else {
         humane.log(result);
      }
   });
   
}

var performImportation = function(){
   
   var thepayload = $('#importbox').val()
   
   jormrpc.invoke('importation', thepayload, function(result){
      humane.log(['importation finished', 'check nintype for more info']);
   });

};

var selectLogTextbox = function(){
   $('#logtextbox').focus();
   $('#logtextbox').select();
}

var requestLog = function(logname){
   jormrpc.invoke('requestlog', logname, function(result){
      $('#logtextbox').html(result);
   });
}

var requestExport = function(exportname){
   jormrpc.invoke('requestexport', exportname, function(result){
      humane.log(['hey']);
      $('#exportbox').val(result);
   });
}

var getCurrentTheme = function(){
   
   jormrpc.invoke("getthemescript", function(result){
      $('#themebox').val(result);
   });
   
}

var applyTheme = function(){
   jormrpc.invoke("readthemescript", $('#themebox').val(), function(result){
      // nothing, I guess?
   });
}

var importWordlist = function(){
   jormrpc.invoke("receivewordlist", $('#wordlistname').val(), $('#wordlistbox').val(), function(result){
      // yeah, what
   });
}

$(document).ready(function(){

   // here here
   
   {
   
      var i;
      for(i = 0; i < 10; i++){
      
         $("#colorlist").append("Hey!");
      
         var theinput = $('<input>').attr({
            name: 'bar'
         }).appendTo("#colorlist");
         
         theinput.spectrum({
            showAlpha: true,
            showInput: true,
            color: "#ff960064"
         });
         
         $('<br>').appendTo("#colorlist");
         
      }
   }

   initiatejormrpc();
   
   karbase.onactivate = function(thekarl){
      console.log("karbase activated : " + thekarl);
      
      
      if(thekarl == "themekarl"){
         console.log("Themekarl activated!");
         // here here
         // here here
      }
      
   };
   
   karbase.activate("loginkarl");
   
   $('#leconsole').css('text-align', 'left').css('overflow', 'auto');
   
   var leterm = $('#leconsole').terminal(function(command, term){
         jormrpc.invoke("console", command, function(result){
            term.echo(result);
         });
      }, {
      prompt: '[[;red;]nintype] [[;gold;]>>] ',
      width: '100%',
      outputlimit: 40,
      greetings: '[[b;#333;]NINTYPE CONSOLE (type help for help)] [[;teal;] ><((o>]',
      height: 500,
      completion: function(leterm, lestring, callback){
         jormrpc.invoke("consolecompletion", lestring, function(result){
            callback(result.split(" "));
         });
      }
   });
   
   
   console.log("gotshere right?");
   
   $('#wholething').contextPopup({
      title: 'NAV',
      maximumy: 50,
      leftclicktoo : true,
      items: [
         {label: 'LOGIN',  action: function() { karbase.activate("loginkarl"); } },
         {label: '-------'},
         {label: 'CONSOLE',  action: function() { karbase.activate("consolekarl"); } },
         {label: 'Console Import Box',  action: function() { karbase.activate("consoleimport"); } },
         {label: 'Console Export Box',  action: function() { karbase.activate("consoleexport"); } },
         {label: '-------'},
         {label: 'IMPORT',  action: function() { karbase.activate("importkarl"); } },
         {label: 'EXPORT',  action: function() { karbase.activate("exportkarl"); } },
         {label: 'WORDLIST',  action: function() { karbase.activate("wordlistkarl"); } },
         {label: '-------'},
         {label: 'THEME',  action: function() { karbase.activate("themekarl"); } },
         {label: 'OPTIONS',  action: function() { karbase.activate("optionskarl"); } },
         {label: '-------'},
         {label: 'LOGS',  action: function() { karbase.activate("logskarl"); } },
         {label: '-------'},
         {label: 'RELAY',  action: function() { karbase.activate("relaykarl"); } },
      ]
   });
   
   $('#typebox').keydown(function(e){
   
      var shifted = e.shiftKey;
      var controlled = e.ctrlKey;
      var alted = e.altKey;
      
      var modifier = "";
      if(shifted)
         modifier = modifier + "s";
      if(controlled)
         modifier = modifier + "c";
      if(alted)
         modifier = modifier + "a";
      
      
      var doit = true;
      
      var keytype;
      
      if(e.keyCode == 8){
         keytype = "bksp";
      } else if (e.keyCode == 37){
         keytype = "left";
      } else if (e.keyCode == 38){
         keytype = "up";
      } else if (e.keyCode == 39){
         keytype = "right";
      } else if (e.keyCode == 40){
         keytype = "down";
      } else if (e.keyCode == 33){
         keytype = "pgup";
      } else if (e.keyCode == 34){
         keytype = "pgdn";
      } else if (e.keyCode == 35){
         keytype = "end";
      } else if (e.keyCode == 36){
         keytype = "home";
      } else if (e.keyCode == 46){
         keytype = "del";
      } else if (e.keyCode == 9){
         keytype = "tab";
      } else {
         doit = false;
      }
      
      if(doit){
         jormrpc.invoke("type", "spec", keytype, modifier, function(result){
            // well, whatever yeah
         });
      }
      
      
      
   });
   
   $('#typebox').bind('keypress', function(e){
      
      var code = (typeof e.which == "number") ? e.which : e.keyCode;
      if(code){
         jormrpc.invoke("type", "char", String.fromCharCode(code), "", function(result){
            leterm.echo("oh yeah : " + result + " :: " + String.fromCharCode(code));
         });
         
         $('#typebox').val('');
      }
      
   });
   
   
});
