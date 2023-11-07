<#-- $This file is distributed under the terms of the license in LICENSE$ -->

<#-- This is an example of how we could call this macro from a custom form if we end up
implementing this like the other freemarker "widgets". For now I just have this here as
an example and I didn't really see any other way to get the jsp custom forms to call
this at the moment. Needless to say, we need to be able to call the macro and pass
parameters from within the original custom form -->

<#--
Available variables:

fieldName -- name of field
minimumPrecision -- minimum precision accepted by validator
requiredLevel -- maximum precision to display as required

existingPrecision -- precision on an existing value, may be ""
year -- year on an existing value, may be ""
month -- month on an existing value, may be ""
day  -- day on an existing value, may be ""
hour -- hour on an existing value, may be ""
minute -- minute on an existing value, may be ""
second -- second on an existing value, may be ""

precisionConstants.none -- URI for precision
precisionConstants.year -- URI for precision
precisionConstants.month -- URI for precision
precisionConstants.day -- URI for precision
precisionConstants.hour -- URI for precision
precisionConstants.minute -- URI for precision
precisionConstants.second -- URI for precision
-->

<input type="date" placeholder="TT.MM.JJJJ" name="${fieldName}" <#if isodate??>value="${isodate}"</#if> class="required" required>
