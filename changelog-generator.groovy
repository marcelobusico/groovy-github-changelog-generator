/**
 * Changelog Generator using Git and Github.
 * Create by Marcelo Busico on 2016-06-24.
 */

println ""
println "--------------------------------"
println "Executing ChangeLog Generator..."
println "--------------------------------"
println ""

def user = System.getenv("GITHUB_USERNAME")
def token = System.getenv("GITHUB_TOKEN")

if(!user || !token) {
    println "You need to define the following environment variables before using this script: GITHUB_USERNAME, GITHUB_TOKEN"
    return
}

if(args.length != 3) {
    println "Usage: groovy changelog-generator.groovy GITHUB_REPO_URL TAG_START TAG_END"
    return
}

def repoUrl = args[0]
def tagStart = args[1]
def tagEnd = args[2]


println "Github Repo URL: ${repoUrl}"
println "Start Tag: ${tagStart}"
println "End Tag: ${tagEnd}"
println ""


println "Getting commits from current local Git working copy..."
def logCmd = "git log ${tagStart}..${tagEnd} --pretty=format:%H --merges"
def logResult = logCmd.execute().text
def commits = logResult.split("\\r?\\n")
println "Done."



println "Getting Pull Requests from Github..."
def slurper = new groovy.json.JsonSlurper()
def prsUrl = new URL("${repoUrl}/pulls?state=all")
def encodedAuth = "${user}:${token}".getBytes().encodeBase64().toString()
URLConnection connection = prsUrl.openConnection();
connection.setRequestProperty("Authorization", "Basic ${encodedAuth}");
def prResponse = slurper.parse(new BufferedReader(new InputStreamReader(connection.getInputStream())))
println "Done."



println "Processing changelog..."
StringBuilder sb = new StringBuilder()

sb.append("# Change Log")
sb.append("\n\n")

sb.append("## Version ${tagEnd}:")
sb.append("\n\n")

sb.append("**Merged Pull Requests:**")
sb.append("\n\n")

for(def commitSha : commits) {
    for(def pr in prResponse) {
	if(commitSha.equals(pr.merge_commit_sha)) {
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
