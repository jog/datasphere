<!------------- PANELS SECTION --------------->
<div id="right">
	<!------------- details panel ---------------->
	<div class="panelItem" >
		<div class="heading">
			Your datasphere details:
		</div>
		<div class="box" style="height:65px;">
			name:
			<div class="purple"> ${ user.getFirstname() } ${ user.getLastname() } </div>
			datasphere id:
			<div class="purple"> ${ user.getJid() }</div>
		</div>
	</div>
	
	<!------------- active dataware panel --------->
	<div class="panelItem" >
		<div class="heading">
			Dataware you are running:
		</div>
		<div class="box">
			<#list activeSubs as activeSub>
				<a href="/source_history?jid=${ user.getJid() }&ns=${ activeSub.getNamespace() }">
				<img class="dwicon-small" src="/static/images/icons/${ activeSub.getAvatarName() }"/></a>
			</#list>
		</div>
	</div>
	
	<!------------- pending dataware panel --------->
	<div class="panelItem" >
		<div class="heading">
			Dataware requests:
		</div>
		<div class="box">
			<#list pendingSubs as activeSub>
				<a href="/source_history?jid=${ user.getJid() }&ns=${ activeSub.getNamespace() }">
				<img class="dwicon-small" src="/static/images/icons/${ activeSub.getAvatarName() }"/></a>
			</#list>
		</div>
	</div>
	
	<!------------- now available panel ------------>
	<div class="panelItem" >
		<div class="heading">
			Available dataware:
		</div>
		<div class="box" >
			<img class="dwicon-small" src="/static/images/icons/blogger.jpg"/>
			<img class="dwicon-small" src="/static/images/icons/myspace.jpg"/>
			<img class="dwicon-small" src="/static/images/icons/youtube.jpg"/>
			<img class="dwicon-small" src="/static/images/icons/wordpress.jpg"/>
			<img class="dwicon-small" src="/static/images/icons/reddit.jpg"/>
			<img class="dwicon-small" src="/static/images/icons/skype.jpg"/>
			<img class="dwicon-small" src="/static/images/icons/vimeo.jpg"/>
			<img class="dwicon-small" src="/static/images/icons/digg.jpg"/>
			<img class="dwicon-small" src="/static/images/icons/email.jpg"/>
			<img class="dwicon-small" src="/static/images/icons/gmail.jpg"/>
			<img class="dwicon-small" src="/static/images/icons/spotify.jpg"/>
			<img class="dwicon-small" src="/static/images/icons/github.jpg"/>
			<img class="dwicon-small" src="/static/images/icons/twitter.jpg"/>
			<img class="dwicon-small" src="/static/images/icons/brightkite.jpg"/>
			<img class="dwicon-small" src="/static/images/icons/newsvine.jpg"/>
			<img class="dwicon-small" src="/static/images/icons/gtalk.jpg"/>
			<img class="dwicon-small" src="/static/images/icons/gowalla.jpg"/>
			<img class="dwicon-small" src="/static/images/icons/foursquare.jpg"/>
			<img class="dwicon-small" src="/static/images/icons/picasa.jpg"/>
			<a href="http://data-chant.appspot.com/main">
				<img class="dwicon-small" src="/static/images/icons/facebook.jpg"/>
			</a>
		</div>
		<div class="more">
			<a href="">see all...</a>
		</div>
	</div>
</div>