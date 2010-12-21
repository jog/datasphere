<#include "header.ftl">

<#include "side_menu.ftl">
<#include "side_panels.ftl">


<!------------- UPDATES SECTION --------------->
<div id="middle">
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
</div> 

<#include "footer.ftl">