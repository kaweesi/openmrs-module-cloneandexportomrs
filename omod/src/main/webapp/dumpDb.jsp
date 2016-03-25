<%@ include file="/WEB-INF/template/include.jsp"%>
<%@ include file="/WEB-INF/template/header.jsp"%>

<%@ include file="template/localHeader.jsp"%>

Requires mysqldump to be accessible on terminal and only works for unix

<form method="post">
	<input type="submit" value="Dump Database Now">
</form>

<%@ include file="/WEB-INF/template/footer.jsp"%>