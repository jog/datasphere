<#include "header.ftl">

<#include "side_menu.ftl">
<#include "side_panels.ftl">

<!------------- SCRIPTS --------------->
<script>

	function changeSubscription( action ) {
		
		jQuery.ajax({ 
			type : "GET", 
			url : "subscription?action=" + action + "&jid=${user.getJid()}&ns=${source.getNamespace()}",
			cache : false,
			async : true,
			dataType : "json",
			contentType : "json",
			timeout : 10000,			
			error : function( data ) { alert( "error"); }, 
			success : function( data ) { 
				
				if ( data.success == true ) {
					alert( action + " was successful" )
					location.reload( true );
				}
				else 
					alert( action + " has failed." )
			}
		});
	}

</script>

<!------------- PARAMETER SECTION --------------->
<#if subscription??>
	<#assign status = subscription.getSubscriptionStatus()>
<#else>
	<#assign status = "NONE">
</#if>

<!------------- SOURCE SECTION --------------->
<div id="middle">
	<div class="panelItem" style="margin-top:15px;">
		<div class="heading">
			Dataware Source summary:
		</div>
		<div class="box">
			<div style="float:left">
				<a href="${ source.getUrl() }" target="_blank" >
					<img class="dwicon" src="/static/images/icons/${ source.getIcon() }"/>
				</a>
			</div>
			<div style="margin-left:40px;">
				common name:
				<div class="purple">${ source.getCommonName() } </div>
				namespace:
				<div class="purple">${ source.getNamespace() } </div>

				subscription status:				

				<#if status == "REJECTED" >
					<div class="red">Dataware permanently rejected.</div>
					<a href="javascript:changeSubscription('RESET')">reset subscription</a>
				
				<#elseif status == "EXPECTED" >						
					<div class="purple">Ready to auto-accept dataware.</div>
					<a href="javascript:changeSubscription('RESET')">reset subscription</a>
					
				<#elseif status == "RECEIVED" >
					<div class="purple">Request received. Awaiting your response. </div>
					<a href="javascript:changeSubscription('ACCEPT')">accept subscription</a> |
					<a href="javascript:changeSubscription('REJECT')">reject subscription</a>

				<#elseif status == "ACCEPTED" >
					<div class="purple">Request accepted. Trying to send response. </div>
					<a href="javascript:changeSubscription('RESET')">cancel accept</a> |
					<a href="javascript:changeSubscription('RETRY')">retry</a>

				<#elseif status == "RESPONDED" >
					<div class="purple">Request accepted. Awaiting completion.</div>
					<a href="javascript:changeSubscription('RESET')">cancel accept</a>

				<#elseif status == "COMPLETED" >
					<div class="green">ACTIVE </div>
					<a href="javascript:changeSubscription('UNSUBSCRIBE')">unsubscribe</a>
									
				<#else>
					<div class="purple">none</div>
					<a href="javascript:changeSubscription('RESET')">set to auto-accept</a>
				</#if>
			</div>
		</div>			
	</div>
	
	<#if status == "COMPLETED" >
	<div style="height:20px; margin-top:12px; margin-bottom:-8px;">
		<div style="float:right;"> 
		${ paginator.generateHTML() }
		</div>
	</div>
	<#assign currentDate = "">
	<#list updates as update>
		<#if currentDate != update.getCtimeAsDate() >
			<#assign currentDate = update.getCtimeAsDate()>
			<div class="day">${ currentDate }</div>
		</#if>
			<div class="entry">
				<img src="http://www.globalvolunteernetwork.org/image/facebook-icon.gif"/>
				<div class="update"> 
					${ update.description } 
					<span class="time"> ${ update.getCtimeAsTime() } </span>
				</div>
			</div>
	</#list>
	</#if>

<#include "footer.ftl">
