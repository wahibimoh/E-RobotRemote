(function () {
if( typeof window.res !== 'undefined') return;

window.res = 1;
for(div of $("div:contains('test_123')")) if(div.innerHTML == 'test_123') res = div

function time() {
  var d = new Date();
  var s = d.getSeconds();
  var m = d.getMinutes();
  var h = d.getHours();
  res.innerHTML = 
    ("0" + h).substr(-2) + ":" + ("0" + m).substr(-2) + ":" + ("0" + s).substr(-2);
}

setInterval(time, 1000);
})();
