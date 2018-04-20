# nexus-sync
Sync between two sonatype nexus repositories

Until sonatype nexus 2, All maven artifact is just file and folder, so just copy or upload `storage` folder for sync to respositories

But nexus 3 stores artifact into DB. So only sync option is calling web API, sadly.

So I have made this script.


nexus 2 까지는 모든 파일이 단순히 파일 폴더 였기 때문에  단순이 storage 폴더를 다른 넥서스 설치 위치에 업로드 하면 동기화가 되었다.

nexus 3 부터는 DB화 되었기 때문에 nexus에서 제공하는 API를 이용해야만 정상적으로 동기화 할 수 있다.

본 스크립트는 이런 문제를 해결하기 위해 만들었다.

# Usage:

```groovy nexusSync.groovy [type] [sourceUrl] [toUrl]```

* type:      maven or npm
* sourceUrl: sync source. must end with '/'
* toUrl:     sync target. must end with '/'

# Example:

```groovy nexusSync.groovy maven http://localhost:8081/nexus/maven-public/ http://my-private-nexus.com/nexus/private_repository/```
