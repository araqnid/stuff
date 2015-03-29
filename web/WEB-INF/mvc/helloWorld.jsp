<%@ page contentType="text/html" pageEncoding="UTF-8" session="false" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<html>
<head>
<title>hello world</title>
</head>
<body>
<h1>MVC hello world</h1>
<p>forwarded onto here by controller</p>
<p><c:out value="${message}" /></p>
</body>
</html>
