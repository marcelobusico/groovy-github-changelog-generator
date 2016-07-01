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

def includePullInLabel(def pullByLabel, String label, def pr) {    
    String labelToUse
    if(pullByLabel.keySet().contains(label)) {
	labelToUse = label
    } else {
	labelToUse = "GENERIC_LABEL"
    }
    
    pullByLabel[labelToUse] += pr
    
    return pullByLabel
}

def getLabelTitleMappings() {
    def defaultMappingJson = '{"mappings":[{"label":"analytics","title":"Analytics changes"},{"label":"bug","title":"Bug fixes"},{"label":"crash","title":"Crash fixes"},{"label":"enhancement","title":"Enhancements"},{"label":"translation","title":"Translations"},{"label":"GENERIC_LABEL","title":"Merged Pull Requests"}]}'

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
    def title = "Unknown title"
    
    for(def labelTitleMapping : labelTitleMappings) {
	if(label.equals(labelTitleMapping.label)) {
	    title = labelTitleMapping.title
	    break;
	}
    }
    
    return title
}

def executeGenerator() {
    println ""
    println "--------------------------------"
    println "Executing ChangeLog Generator..."
    println "--------------------------------"
    println ""

    

    if(!getGithubUser() || !getGithubToken()) {
	println "You need to define the following environment variables before using this script: GITHUB_USERNAME, GITHUB_TOKEN and optionally GITHUB_API."
	return
    }

    //Verify Arguments
    if(args.length != 2) {
	println "Usage: groovy ChangelogGenerator.groovy TAG_START TAG_END"
	return
    }
    
    //Arguments
    def tagStart = args[0]
    def tagEnd = args[1]
    def githubApi = getGithubApi()

    
    //Determine Repo Name 
    //using first result of 'git remote' of local git working copy.
    def gitRemoteCmd = "git remote -v"
    def gitRemoteResult = gitRemoteCmd.execute().text
    def gitRemotes = gitRemoteResult.split("\\r?\\n")
    
    if(!gitRemotes) {
	println "This git working copy has not any remote configured."
	return
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
    println "Start Tag: ${tagStart}"
    println "End Tag: ${tagEnd}"
    println "Label Mappings: ${labelTitleMappings}"
    println ""


    //Get Local Git Data
    println "Getting commits from current local Git working copy..."
    def logCmd = "git log ${tagStart}..${tagEnd} --pretty=format:%H --merges"
    def logResult = logCmd.execute().text
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
    def pullsByLabel = [:]
    for(def labelTitleMapping : labelTitleMappings) {
	pullsByLabel[labelTitleMapping.label] = []
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
			    if(label.name in pullsByLabel.keySet()) {
			        //PR label is one of the mapped one.
				isIncludedInAtLeastOneLabel = true
				includePullInLabel(pullsByLabel, "${label.name}", pr)			      
			    }
			}
		    }
		    
		    if(!isIncludedInAtLeastOneLabel) {
		        //Pull Request does not have any label or those labels are not in the titles map.
		        includePullInLabel(pullsByLabel, "", pr)
		    }
		}
	    }
	}
    }
    
    
    println "Processing changelog..."
    StringBuilder sb = new StringBuilder()

    sb.append("# Change Log")
    sb.append("\n\n")

    sb.append("## [${tagEnd}](${repoInfoResponse.html_url}/tree/${tagEnd}) (${tagEndDate.format('yyyy-MM-dd')})")
    sb.append("\n")

    sb.append("[Full Changelog](${repoInfoResponse.html_url}/compare/${tagStart}...${tagEnd})")
    sb.append("\n")

    for(def pulls : pullsByLabel) {
	if(pulls.value) {
	    //There is at least one PR in this group
	    sb.append("\n")
	    sb.append("**${getTitleForLabel(labelTitleMappings, pulls.key)}:**")
	    sb.append("\n\n")

	    for(def pr : pulls.value) {
		sb.append("- ${pr.title} [\\#${pr.number}](${pr.html_url}) ([${pr.user.login}](${pr.user.html_url}))")
		sb.append("\n")
	    }
	}
    }

    String changeLog = sb.toString()
    println "Done."



    println ""
    println ""
    println "Generated Changelog:"
    println "--------------------"
    println ""
    println changeLog
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
    changelogFile.write(changeLog)
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
}

executeGenerator()
