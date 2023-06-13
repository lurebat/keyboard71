// e.g give it "xxxxxxx"
function randomHexes(format){
   var newstr = new String(format);
   newstr = newstr.replace(/[xy]/g, function(c) {
    var r = Math.random()*16|0, v = c == 'x' ? r : (r&0x3|0x8);
    return v.toString(16);
   });
   return newstr;
}

(function(){
   var entityMap = {
       "&": "&amp;",
       "<": "&lt;",
       ">": "&gt;",
       '"': '&quot;',
       "'": '&#39;',
       "/": '&#x2F;'
     };
   
   window.escapeHtml = function(string) {
     return String(string).replace(/[&<>"'\/]/g, function (s) {
       return entityMap[s];
     });
   }
}());
