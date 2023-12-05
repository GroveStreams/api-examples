<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<title>GroveStreams Simple Feed PUT Example</title>
 
<script type="text/javascript"
        src="http://ajax.googleapis.com/ajax/libs/jquery/1.3.2/jquery.min.js"></script>
 
</head>
 
<script type="text/javascript">

//Assemble GS Request
var apiKey = '21c07b54-9426-30e2-b288-eaebf0916717'; //Change This!!!
var compId = encodeURIComponent('motions[0]'); 		 // Change This!!! Your component ID.
var streamId = encodeURIComponent('motion');         // Change This!!! The components stream ID

function getLastValue(successFn) {

    // Use https if your domain is using SSL
    var url = 'http://grovestreams.com/api/comp/' + compId + '/stream/' + streamId + '/last_value?api_key=' + apiKey;

    $.ajax({
                type : 'GET',
                url : url,
                dataType : 'json',
                cache : false,
                processData : true,
	        crossDomain : true,
                async : true,
                success : function(jfeed) {
                        successFn(jfeed);
                },
                error : function(a) {
                        //alert("Error");
                }
        });
}

function getLastValueFormatted(successFn) {

    // Use https if your domain is using SSL
    var url = 'http://grovestreams.com/api/comp/' + compId + '/stream/' + streamId + '/last_value?retFData&api_key=' + apiKey;

    $.ajax({
                type : 'GET',
                url : url,
                dataType : 'json',
                cache : false,
                processData : true,
	        crossDomain : true,
                async : true,
                success : function(jfeed) {
                        successFn(jfeed);
                },
                error : function(a) {
                        //alert("Error");
                }
        });
}

function getLast10(successFn) {
	//If you want to pass a start data and end date then use the sd and ed parameters set to epoch millis
	// For example, getting the current time in epoch millis is done like this:
	//   var now = new Date();
	//   var nowMillis = now.getTime();

    // Use https if your domain is using SSL
    var url = 'http://grovestreams.com/api/comp/' + compId + '/stream/' + streamId + '/feed?limit=10&api_key=' + apiKey;

    $.ajax({
                type : 'GET',
                url : url,
                dataType : 'json',
                cache : false,
                processData : true,
	        crossDomain : true,
                async : true,
                success : function(jfeed) {
                        successFn(jfeed);
                },
                error : function(a) {
                        //alert("Error");
                }
        });
}

function formatAMPM(date) {
        //Avoiding including another large scripting library so added this method
        var hours = date.getHours();
        var minutes = date.getMinutes();
        var ampm = hours >= 12 ? 'pm' : 'am';
        hours = hours % 12;
        hours = hours ? hours : 12; // the hour '0' should be '12'
        minutes = minutes < 10 ? '0' + minutes : minutes;
        var strTime = hours + ':' + minutes + ' ' + ampm;
        return strTime;
}

//Start (trigged by onload() below)
function init() {
    getLastValue(function(jfeed) {
    	var tuple = jfeed[0]; //one tuple should be returned 
    	
    	//Set the divs below to the result
    	$('#lv_time').text('Time: ' + formatAMPM( new Date(tuple.time)));
    	
    	$('#lv_value').text('Value: ' + tuple.data);
    	
    });
    
    getLastValueFormatted(function(jfeed) {
    	var tuple = jfeed[0]; //one tuple should be returned 
    	
    	//Set the divs below to the result
    	$('#lvf_time').text('Time: ' + formatAMPM( new Date(tuple.time)));
    	
    	$('#lvf_value').text('Value: ' + tuple.fData);
    	
    });
    
    getLast10(function(jfeed) {
    	
    	for (var i=0; i<jfeed.length; i++) {
    		var tuple = jfeed[i];
    		$('#l10_values').append('<div>Time: ' + formatAMPM( new Date(tuple.time)) + ' Value: ' + tuple.data + '</div>');
    	}
    	
    });
}
</script>

<body onload=init()>
<div style="font-size: 13px; font-family: Verdana; padding: 10px 0 0 30px">
    <div style="font-weight:bold">Last Value:</div>
    <div id="lv_time"></div>
    <div id="lv_value"></div>
    <br/>
    <div style="font-weight:bold">Last Value Formatted With Unit:</div>
    <div id="lvf_time"></div>
    <div id="lvf_value"></div>
                
    <br/>
    <div style="font-weight:bold">Last 10 values:</div>
    <div id="l10_values"></div>
    
</div>
</body>
 
</html>
