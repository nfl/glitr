$: << './'


if Gem::Specification::find_all_by_name('bundler').empty?
  sh "gem install 'bundler'"
  abort "Bundler was missing so it was installed. Please re-run the script to continue."
end

sh "bundle install"

require 'rubygems'
require 'bundler/setup'
Bundler.require(:default)


require 'opsscripts_java/tasks'

task :nfl_build => [
    :nfl_build_init_seam,
    'gradle:build',
    'gradle:test',
    'gradle:sonar_qube',
    'common:package'
]
task :nfl_deploy => [
    'gradle:upload_archives'
]
