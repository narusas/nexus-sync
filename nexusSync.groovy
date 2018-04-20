@Grab('org.codehaus.groovy.modules.http-builder:http-builder:0.7.1')

import groovyx.net.http.RESTClient
import static groovyx.net.http.ContentType.*


def printUsage(){
  println """Usage:
  groovy nexusSync.groovy [type] [sourceUrl] [toUrl]

  type:      maven or npm
  sourceUrl: sync source. must end with '/'
  toUrl:     sync target. must end with '/'

Example:
  groovy nexusSync.groovy maven http://localhost:8081/nexus/maven-public/ http://my-private-nexus.com/nexus/private_repository/
"""
}

if (args.length != 3){
  printUsage();
  return;
}

def type = args[0];
if ('maven' != type && 'npm' != type){
  printUsage();
  return;
}


def isValidUrl(url) {
  return url.startsWith('http') && url.endsWith('/');
}

def sourceFullUrl = args[1];
def targetFullUrl = args[2];

if (sourceFullUrl == targetFullUrl || ! isValidUrl(sourceFullUrl) || ! isValidUrl(targetFullUrl)){
  println "Invalid url"
  printUsage();
  return;
}

def sourceRepository = sourceFullUrl.split('/')[-1]
def targetRpository = targetFullUrl.split('/')[-1]

def leftUrl = sourceFullUrl[0..(sourceFullUrl.lastIndexOf(sourceRepository)-1)]
def rightUrl = targetFullUrl[0..(targetFullUrl.lastIndexOf(targetRpository)-1)]



println "Source Url: ${leftUrl} Repository: ${sourceRepository}"
println "Target Url: ${rightUrl} Repository: ${targetRpository}"

def repositories = [
	[from:sourceRepository, to:targetRpository, type: type],

]


def fetch = { url, repository->

	def context = url.substring(url.lastIndexOf('/'))

	def restClient = new RESTClient(url)
	def continuationToken = null ;
	def items = []
	def path = "${context}/service/rest/beta/assets".replace('//','/').replace('//','/').replace('//','/')

	while(true) {
		def query = ['repository':repository]

		if (continuationToken!=null) {
			query['continuationToken'] = continuationToken;
		}
		println query
		def response = restClient.get(
			path: path ,
			contentType: JSON,
			query:query
		)


		items.addAll(response.data.items)
		continuationToken = response.data['continuationToken']
		if (continuationToken == null) {
			break;
		}

	}
	return items;
}




def convert = {
	return [
			'path':it.path,
			'downloadUrl':it.downloadUrl,
			'checksum': it.checksum.sha1
		]}

def fails  = []
repositories.each { repository ->
	def lefts = fetch(leftUrl, repository.from).collect(convert)

	def rights = fetch(rightUrl, repository.to) .collect(convert)
	def remains = []
	lefts.findAll { leftItem ->
		def matched = rights.find { rightItem ->
			return leftItem.path == rightItem.path && leftItem.checksum == rightItem.checksum
		}
		if (matched == null){
			remains += leftItem;
		}

	}


	remains.each {
		println "Mismtached: ${it}"

		def fileName =  "${it.downloadUrl.substring(it.downloadUrl.lastIndexOf('/')+1)}";
		def downloadCmd = "curl ${it.downloadUrl} --output ${it.downloadUrl.substring(it.downloadUrl.lastIndexOf('/')+1)}"
		if (repository.type == 'maven'){
			println downloadCmd
			println downloadCmd.execute().text

			if (new File(fileName).exists()==false || new File(fileName).length() ==0){
				println "!! Fail to download ${fileName}"
				fails += fileName
				return;
			}

			def leftPath = "/repository/${repository.from}/"
			def subpath = it.downloadUrl.substring(it.downloadUrl.indexOf(leftPath)+leftPath.length());

			def rightPath = "/repository/${repository.to}/"

			def uploadUrl = "${rightUrl}/${rightPath[1..-1]}${subpath}"
			def uploadCmd = "curl -v -u admin:admin123 --upload-file ${fileName} ${uploadUrl}"
			println uploadCmd
			println uploadCmd.execute().text
		}
		else if (repository.type == 'npm' && fileName.endsWith("gz")){
			println downloadCmd
			println downloadCmd.execute().text

			if (new File(fileName).exists()==false || new File(fileName).length() ==0){
				println "!! Fail to download ${fileName}"
				fails += fileName
				return;
			}

			def uploadUrl = "${rightUrl}/repository/${repository.to}/"
			def uploadCmd = "npm --registry ${uploadUrl} publish ${fileName}"
			println uploadCmd
			println uploadCmd.execute().text
		}


	}


}
println "-----------------------------------"
println "Failed List"
println "-----------------------------------"
fails.each { println it}
