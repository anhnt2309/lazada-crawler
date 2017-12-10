importClass("us.originally.lazadacrawler.activitiest.TestRef")
  var data = {};
  data.count =  10;
  data.getItem = function(index){
         return "Javascript Data " + index;
  }
  TestRef.showData(data);