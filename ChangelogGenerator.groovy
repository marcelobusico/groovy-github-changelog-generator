/**
 * Changelog Generator using Git and Github.
 * Create by Marcelo Busico on 2016-06-24.
 */

def getGithubUser() {
    def user = System.getenv("GITHUB_USERNAME")
    return user
}

def getGithubToken() {
    def token = System.getenv("GITHUB_TOKEN")
    return token
}

def getGithubApi() {
    def githubApi = System.getenv("GITHUB_API")
    if(!githubApi) {
	githubApi = "https://api.github.com"
    }
    return githubApi
}

def getResourceFromGithub(String repoApiUrl, String resource) {
    def url = "${repoApiUrl}${resource}"
    
    println "Getting resource from Github: '${url}'"
    
    def slurper = new groovy.json.JsonSlurper()
    def user = getGithubUser()
    def token = getGithubToken()        
    def encodedAuth = "${user}:${token}".getBytes().encodeBase64().toString()
	
    URLConnection connection = new URL(url).openConnection()
    connection.setRequestProperty("Authorization", "Basic ${encodedAuth}")
    def response = slurper.parse(new BufferedReader(new InputStreamReader(connection.getInputStream())))
    
    println "Done."
    println ""
    
    return response
}

def postDataToGithub(String repoApiUrl, String resource, def data) {
    def url = "${repoApiUrl}${resource}"
    def jsonBody = groovy.json.JsonOutput.toJson(data)
    
    println "Posting data to Github: '${url}'\nBody: ${jsonBody}"
    
    def slurper = new groovy.json.JsonSlurper()
    def user = getGithubUser()
    def token = getGithubToken()        
    def encodedAuth = "${user}:${token}".getBytes().encodeBase64().toString()
	
    URLConnection connection = new URL(url).openConnection()
    connection.setDoOutput(true)
    connection.setRequestMethod("POST")
    connection.setRequestProperty("Authorization", "Basic ${encodedAuth}")
    connection.setRequestProperty("Content-Type", "application/json")
    
    connection.outputStream.withWriter { Writer writer ->
        writer << jsonBody
    }
    
    def response = slurper.parse(new BufferedReader(new InputStreamReader(connection.getInputStream())))
    
    println "Done."
    println ""
    
    return response
}

def includePullInLabel(def pullByTitle, def labelTitleMappings, String labelTitle, def pr) {    
    String titleToUse
    if(labelTitle && pullByTitle.keySet().contains(labelTitle)) {
	titleToUse = labelTitle
    } else {
	titleToUse = getTitleForLabel(labelTitleMappings, "GENERIC_LABEL")
    }
    
    pullByTitle[titleToUse] += pr
    
    return pullByTitle
}

def getLabelTitleMappings() {
    def defaultMappingJson = '{"mappings":[{"labels":["analytics"],"title":"Analytics changes"},{"labels":["bug","problem"],"title":"Bug fixes"},{"labels":["crash"],"title":"Crash fixes"},{"labels":["enhancement"],"title":"Enhancements"},{"labels":["translation"],"title":"Translations"},{"labels":["GENERIC_LABEL"],"title":"Merged Pull Requests"}]}'

    def slurper = new groovy.json.JsonSlurper()
    def labelMappingsFile = new File('changelog-label-mappings.json')
    
    def labelMappingJsonContent
    if(labelMappingsFile.exists()) {
        //Use Mapping File
	labelMappingJsonContent = new File('changelog-label-mappings.json').text
    }else {
        //Use default mapping
	labelMappingJsonContent = defaultMappingJson
    }
    
    def labelMappings = slurper.parseText(labelMappingJsonContent)

    return labelMappings.mappings
}

def getTitleForLabel(def labelTitleMappings, String label) {
    def title = null
    
    for(def labelTitleMapping : labelTitleMappings) {
        for(def labelConf : labelTitleMapping.labels) {
	    if(label.equals(labelConf)) {
		title = labelTitleMapping.title
		break;
	    }
	}

    }
    
    return title
}

String getCommandLineArgumentValueForKey(String[] args, String argumentName) {
    String argumentValue = null
    String argumentRegex = "^--${argumentName}=(.+)\$"    
    
    for(String arg : args) {
	if(arg =~ /${argumentRegex}/) {
	    argumentValue = (arg =~ /${argumentRegex}/)[0][1]
	    break;
	}
    }
    
    return argumentValue
}

def executeGenerator() {
    println ""
    println "--------------------------------"
    println "Executing ChangeLog Generator..."
    println "--------------------------------"
    println ""

    

    if(!getGithubUser() || !getGithubToken()) {
	println "You need to define the following environment variables before using this script: GITHUB_USERNAME, GITHUB_TOKEN and optionally GITHUB_API."
	System.exit(1)
    }

    
    //Arguments
    String tagStart = getCommandLineArgumentValueForKey(args, "start-tag")
    String tagEnd = getCommandLineArgumentValueForKey(args, "end-tag")
    boolean createGithubRelease = getCommandLineArgumentValueForKey(args, "create-github-release") == "true"

    
    //Get Github API URL
    def githubApi = getGithubApi()
    
    
    //Determine Repo Name 
    //using first result of 'git remote' of local git working copy.
    def gitRemoteCmd = "git remote -v"
    def gitRemoteResult = gitRemoteCmd.execute().text
    def gitRemotes = gitRemoteResult.split("\\r?\\n")
    
    if(!gitRemotes) {
	println "This git working copy has not any remote configured."
	System.exit(1)
    }
    
    def firstRemote = gitRemotes[0]    
    //Trim remote name and only keep repo name (supports Github HTTPS and SSH protocols)
    def githubRepoName = (firstRemote =~ /(.+)[\/:](.+\/.+).git(.+)/)[0][2]
    //Generate Repo API Url    
    def repoApiUrl = "${githubApi}/repos/${githubRepoName}"

    //Load Label Mappings
    def labelTitleMappings = getLabelTitleMappings()
    
    //Print environment data
    println "Repo API URL: ${repoApiUrl}"
    println "Start Tag: ${(tagStart ?: "<Since latest GitHub release>")}"
    println "End Tag: ${(tagEnd ?: "<Till latest tag in current git branch>")}"
    println "Create Git Release: ${createGithubRelease}"
    println "Label Mappings: ${labelTitleMappings}"
    println ""
    
    
    //If not set, determine start tag automatically using latest github release.
    if(!tagStart) {
	println "Determining Start Tag from Latest GitHub Release..."
	def releasesResponse = getResourceFromGithub(repoApiUrl, "/releases")
	if(releasesResponse) {
	    tagStart = releasesResponse[0].tag_name
	}
	println "Calculated Start Tag is: ${tagStart?:"<No start tag found, using first commit instead>"}"
	println ""
    }
    
    
    //If not set, determine end tag automatically using latest tag in current git working copy branch.
    if(!tagEnd) {
	println "Determining End Tag from Latest tag in current Git branch..."
	def gitLatestTagCmd = "git describe --abbrev=0 --tags"
	def gitLatestTagResult = gitLatestTagCmd.execute().text
	println "Done."
	println ""
	if(!gitLatestTagResult) {
	    println "ERROR: There is no tag available to be used as end-tag."
	    System.exit(1)
	}
	tagEnd = gitLatestTagResult.split("\\r?\\n")[0]
	println "Calculated End Tag is: ${tagEnd}"
	println ""
    }

    
    //Get Local Git Data
    println "Getting commits from current local Git working copy..."
    def logCmd = "git log ${(tagStart ? tagStart + '..' : '')}${tagEnd} --pretty=format:%H --merges"
    def logResult = logCmd.execute().text

    if(!logResult) {
	println "ERROR: There is no commits between selected tags to generate a release changelog."
	System.exit(1)
    }
    
    def commits = logResult.split("\\r?\\n")
    println "Done."
    println ""
    
    
    //Get GitHub Data
    def repoInfoResponse = getResourceFromGithub(repoApiUrl, "")
    def prResponse = getResourceFromGithub(repoApiUrl, "/pulls?state=closed&page=1&per_page=100")
    def issuesResponse = getResourceFromGithub(repoApiUrl, "/issues?state=closed")
    def tagsResponse = getResourceFromGithub(repoApiUrl, "/tags")
    
    def endTagCommitSha = null    
    for(def tag : tagsResponse) {
	if(tag.name.equals(tagEnd)) {
	    endTagCommitSha = tag.commit.sha
	    break
	}
    }
    
    if(!endTagCommitSha) {
        println "ERROR: Could not determine commit sha for tag '${tagEnd}'"
        System.exit(1)
    }
    
    def tagCommitResponse = getResourceFromGithub(repoApiUrl, "/commits/${endTagCommitSha}")    
    def tagEndDate = Date.parse("yyyy-MM-dd'T'HH:mm:ss'Z'", tagCommitResponse.commit.author.date)

    
    //Include only pull requests for commits of this version.
    def versionPullRequests = []
    for(def commitSha : commits) {
        def githubCommit = getResourceFromGithub(repoApiUrl, "/commits/${commitSha}")

        for (def parent : githubCommit.parents) {
            pr = prResponse.find { it.head.sha == parent.sha }
            if (pr != null) {
                versionPullRequests += pr
            }
        }
    }

    
    //Group pull requests using labels
    def pullByTitle = [:]
    for(def labelTitleMapping : labelTitleMappings) {
	pullByTitle[labelTitleMapping.title] = []
    }
    
    for(def issue : issuesResponse) {
        if(issue.pull_request) {
	    //This issue is a pull request
	    for(def pr : versionPullRequests) {
	        if(pr.url.equals(issue.pull_request.url)) {
		    //This issue is for this pull request
		    
		    boolean isIncludedInAtLeastOneLabel = false
		    
		    if(issue.labels) {
		        //Pull Request has labels
		        for(def label : issue.labels) {
			    String labelTitle = getTitleForLabel(labelTitleMappings, label.name)
			    if(labelTitle in pullByTitle.keySet()) {
			        //PR label is one of the mapped one.
				isIncludedInAtLeastOneLabel = true
				includePullInLabel(pullByTitle, labelTitleMappings, labelTitle, pr)			      
			    }
			}
		    }
		    
		    if(!isIncludedInAtLeastOneLabel) {
		        //Pull Request does not have any label or those labels are not in the titles map.
		        includePullInLabel(pullByTitle, labelTitleMappings, null, pr)
		    }
		}
	    }
	}
    }
    
    
    println "Processing changelog..."
    StringBuilder sbChangelogTitle = new StringBuilder()
    StringBuilder sbChangelogBody = new StringBuilder()

    sbChangelogTitle.append("# Change Log")
    sbChangelogTitle.append("\n\n")

    sbChangelogTitle.append("## [${tagEnd}](${repoInfoResponse.html_url}/tree/${tagEnd}) (${tagEndDate.format('yyyy-MM-dd')})")
    sbChangelogTitle.append("\n")

    sbChangelogBody.append("[Full Changelog](${repoInfoResponse.html_url}/compare/${tagStart}...${tagEnd})")
    sbChangelogBody.append("\n")

    for(def pulls : pullByTitle) {
	if(pulls.value) {
	    //There is at least one PR in this group
	    sbChangelogBody.append("\n")
	    sbChangelogBody.append("**${pulls.key}:**")
	    sbChangelogBody.append("\n\n")

	    for(def pr : pulls.value) {
		sbChangelogBody.append("- ${pr.title} [\\#${pr.number}](${pr.html_url}) ([${pr.user.login}](${pr.user.html_url}))")
		sbChangelogBody.append("\n")
	    }
	}
    }

    String changelogTitle = sbChangelogTitle.toString()
    String changelogBody = sbChangelogBody.toString()
    println "Done."



    println ""
    println ""
    println "Generated Changelog:"
    println "--------------------"
    println ""
    println changelogTitle
    println changelogBody
    println "--------------------"
    println ""
    println ""


    def changelogFile = new File('CHANGELOG.md')
    String currentContent = null
    if(changelogFile.exists()) {
	println "Reading existing CHANGELOG.md file..."
	currentContent = changelogFile.getText()
	println "Done."
	println ""
    }



    println "Writing CHANGELOG.md file..."
    changelogFile.write(changelogTitle)
    changelogFile.append(changelogBody)
    if(currentContent) {
	changelogFile.append("\n")
	currentContent.eachLine { line, count ->
	    if (count > 1) {
		changelogFile.append(line)
		changelogFile.append("\n")
	    }
	}
    }
    println "Done."    



    println ""
    println "-------------------------------"
    println "Changelog generation completed."
    println "-------------------------------"
    println ""
    
    
    if(createGithubRelease) {
        generateGithubRelease(repoApiUrl, tagEnd, changelogBody)
    }
}

def generateGithubRelease(String repoApiUrl, String tagName, String changelog) {
    println ""
    println "--------------------------"
    println "Creating Github Release..."
    println "--------------------------"
    println ""

    postDataToGithub(repoApiUrl, "/releases", [
	"tag_name": tagName,
	"name": tagName,
	"body": changelog
    ])
    
    println ""
    println "-----------------------"
    println "Github release created."
    println "-----------------------"
    println ""    
}

executeGenerator()
