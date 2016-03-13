<spring:htmlEscape defaultHtmlEscape="true" />
<ul id="menu">
	<li class="first"><a
		href="${pageContext.request.contextPath}/admin"><spring:message
				code="admin.title.short" /></a></li>

	<li
		<c:if test='<%= request.getRequestURI().contains("/manage") %>'>class="active"</c:if>>
		<a
		href="${pageContext.request.contextPath}/module/cloneandexportomrs/clone.form"><spring:message
				code="cloneandexportomrs.clone" /></a>
	</li>
	
	<!-- Add further links here -->
</ul>
<h2>
	<spring:message code="cloneandexportomrs.title" />
</h2>
