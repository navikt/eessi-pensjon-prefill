package no.nav.eessi.eessifagmodul.controllers

pipeline {
    agent any
    stages {
        stage("eessi-fagmodul") {
            steps {
                script {
                    if (params.eessiFagmodul) {
                        if (params.eessiFagmodulVersion == "") {
                            withCredentials([string(credentialsId: 'navikt-ci-oauthtoken', variable: 'GITHUB_OAUTH_TOKEN')]) {
                                version = sh(script: 'git ls-remote --tags --refs https://${GITHUB_OAUTH_TOKEN}:x-oauth-basic@github.com/navikt/eessi-pensjon-fagmodul.git | cut -b 52- | sort -n | tail -n 1', returnStdout: true)
                            }
                        } else {
                            version = params.eessiFagmodulVersion
                        }
                        currentBuild.displayName += "eessi-fagmodul:${version}"
                        build([
                                job       : 'nais-deploy-pipeline',
                                wait      : true,
                                parameters: [
                                        string(name: 'APP', value: "eessi-fagmodul"),
                                        string(name: 'REPO', value: "navikt/eessi-pensjon-fagmodul"),
                                        string(name: 'VERSION', value: version),
                                        string(name: 'DEPLOY_REF', value: version),
                                        string(name: 'NAMESPACE', value: params.environment),
                                        string(name: 'DEPLOY_ENV', value: params.environment)
                                ]
                        ])
                    }
                }
            }
        }
        stage("eessi-pensjon-frontend-api") {
            steps {
                script {
                    if (params.eessiPensjonFrontendApi) {
                        if (params.eessiPensjonFrontendApiVersion == "") {
                            withCredentials([string(credentialsId: 'navikt-ci-oauthtoken', variable: 'GITHUB_OAUTH_TOKEN')]) {
                                version = sh(script: 'git ls-remote --tags --refs https://${GITHUB_OAUTH_TOKEN}:x-oauth-basic@github.com/navikt/eessi-pensjon-frontend-api.git | cut -b 52- | sort -n | tail -n 1', returnStdout: true)
                            }
                        } else {
                            version = params.eessiPensjonFrontendApiVersion
                        }
                        currentBuild.displayName += " eessi-pensjon-frontend-api:${version}"
                        build([
                                job       : 'nais-deploy-pipeline',
                                wait      : true,
                                parameters: [
                                        string(name: 'APP', value: "eessi-pensjon-frontend-api"),
                                        string(name: 'REPO', value: "navikt/eessi-pensjon-frontend-api"),
                                        string(name: 'VERSION', value: version),
                                        string(name: 'DEPLOY_REF', value: version),
                                        string(name: 'DEPLOY_ENV', value: params.environment),
                                        string(name: 'NAMESPACE', value: params.environment),
                                        string(name: 'CONTEXT_ROOTS', value: '/callback')
                                ]
                        ])
                    }
                }
            }
        }
        stage("eessi-pensjon-frontend-ui") {
            steps {
                script {
                    if (params.eessiPensjonFrontendUi) {
                        if (params.eessiPensjonFrontendUiVersion == "") {
                            withCredentials([string(credentialsId: 'navikt-ci-oauthtoken', variable: 'GITHUB_OAUTH_TOKEN')]) {
                                version = sh(script: 'git ls-remote --tags --refs https://${GITHUB_OAUTH_TOKEN}:x-oauth-basic@github.com/navikt/eessi-pensjon-frontend-ui.git | cut -b 52- | sort -n | tail -n 1', returnStdout: true)
                            }
                        } else {
                            version = params.eessiPensjonFrontendUiVersion
                        }
                        currentBuild.displayName += " eessi-pensjon-frontend-ui:${version}"
                        build([
                                job       : 'nais-deploy-pipeline',
                                wait      : true,
                                parameters: [
                                        string(name: 'APP', value: "eessi-pensjon-frontend-ui"),
                                        string(name: 'REPO', value: "navikt/eessi-pensjon-frontend-ui"),
                                        string(name: 'VERSION', value: version),
                                        string(name: 'NAMESPACE', value: params.environment),
                                        string(name: 'DEPLOY_REF', value: version),
                                        string(name: 'DEPLOY_ENV', value: params.environment)
                                ]
                        ])
                    }
                }
            }
        }
        stage("pensjon-mottak") {
            steps {
                script {
                    if (params.pensjonMottak) {
                        if (params.pensjonMottakVersion == "") {
                            withCredentials([string(credentialsId: 'navikt-ci-oauthtoken', variable: 'GITHUB_OAUTH_TOKEN')]) {
                                version = sh(script: 'git ls-remote --tags --refs https://${GITHUB_OAUTH_TOKEN}:x-oauth-basic@github.com/navikt/eessi-pensjon-mottak.git | cut -b 52- | sort -n | tail -n 1', returnStdout: true)
                            }
                        } else {
                            version:
                            params.pensjonMottakVersion
                        }
                        currentBuild.displayName += " pensjon-mottak:${version}"
                        build([
                                job       : 'nais-deploy-pipeline',
                                wait      : true,
                                parameters: [
                                        string(name: 'APP', value: "pensjon-mottak"),
                                        string(name: 'REPO', value: "navikt/eessi-pensjon-mottak"),
                                        string(name: 'VERSION', value: version),
                                        string(name: 'NAMESPACE', value: params.environment),
                                        string(name: 'DEPLOY_REF', value: version),
                                        string(name: 'DEPLOY_ENV', value: params.environment)
                                ]
                        ])
                    }
                }
            }
        }
    }
}