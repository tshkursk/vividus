Meta:
    @epic vividus-plugin-web-app

Scenario: Step verification When I hover a mouse over an image with the src '$src'
Given I am on a page with the URL 'https://www.w3schools.com/howto/howto_css_image_overlay.asp'
When I change context to element located `By.xpath(.//div[@class='containerfade'])`
When I hover a mouse over an image with the src '/w3css/img_avatar3.png'
Then an element by the xpath './/div[@class='textfade']' exists

Scenario: Step verification When I hover a mouse over an image with the tooltip '$tooltipImage'
Given I am on a page with the URL 'https://www.w3schools.com/howto/howto_css_image_overlay.asp'
When I change context to element located `By.xpath(.//div[@class='containerfade'])`
When I hover a mouse over an image with the tooltip 'Avatar'
Then an element by the xpath './/div[@class='textfade']' exists

Scenario: Step verification When I click on an image with the src '$src'
Given I am on a page with the URL '${vividus-test-site-url}/index.html'
When I click on an image with the src 'img/vividus.png'
Then number of elements found by `xpath(//a[@href='#ElementId'])` is = `1`


Scenario: Step verification When I click on an image with the name '$imageName'
Given I am on a page with the URL '${vividus-test-site-url}/index.html'
When I click on an image with the name 'vividus-logo'
Then number of elements found by `xpath(//a[@href='#ElementId'])` is = `1`

Scenario: Step verification Then an image with the src '$src' exists
Given I am on a page with the URL 'https://www.w3schools.com/howto/howto_css_image_overlay.asp'
When I change context to element located `By.xpath(.//div[@class='containerfade'])`
Then an image with the src '/w3css/img_avatar3.png' exists

Scenario: Step verification Then a [$state] image with the src '$src' exists
Given I am on a page with the URL 'https://www.w3schools.com/howto/howto_css_image_overlay.asp'
When I change context to element located `By.xpath(.//div[@class='containerfade'])`
Then a [VISIBLE] image with the src '/w3css/img_avatar3.png' exists

Scenario: Step verification Then an image with the src '$src' does not exist
Given I am on a page with the URL 'https://www.w3schools.com/tags/tryit.asp?filename=tryhtml_link_image'
When I switch to frame located `By.id(iframeResult)`
When I click on an image with the src 'logo_w3s.gif'
Then an image with the src 'logo_w3s.gif' does not exist

Scenario: Step verification Then an image with the src containing '$srcpart' exists
Given I am on a page with the URL 'https://www.w3schools.com/howto/howto_css_image_overlay.asp'
When I change context to element located `By.xpath(.//div[@class='containerfade'])`
Then an image with the src containing '3css/img_avatar3.png' exists

Scenario: Step verification Then an image with the tooltip '$tooltip' and src containing '$srcpart' exists
Given I am on a page with the URL '${vividus-test-site-url}/index.html'
Then an image with the tooltip 'Vividus Logo' and src containing 'mg/vividus.png' exists

Scenario: Step verification Then an image with the src '$imageSrc' and tooltip '$tooltip' exists
Given I am on a page with the URL '${vividus-test-site-url}/index.html'
Then an image with the src 'img/vividus.png' and tooltip 'Vividus Logo' exists

Scenario: Step verification Then a [$state] image with the src '$imageSrc' and tooltip '$tooltip' exists
Given I am on a page with the URL 'https://www.w3schools.com/howto/howto_css_image_overlay.asp'
When I change context to element located `By.xpath(.//div[@class='containerfade'])`
Then a [VISIBLE] image with the src '/w3css/img_avatar3.png' and tooltip 'Avatar' exists

Scenario: Step verification Then a [$state] image with the src containing '$srcpart' exists
Given I am on a page with the URL 'https://www.w3schools.com/howto/howto_css_image_overlay.asp'
When I change context to element located `By.xpath(.//div[@class='containerfade'])`
Then a [VISIBLE] image with the src containing '3css/img_avatar3.png' exists

Scenario: Step verification Then a [$state] image with the tooltip '$tooltipImage' exists
Given I am on a page with the URL 'https://www.w3schools.com/howto/howto_css_image_overlay.asp'
When I change context to element located `By.xpath(.//div[@class='containerfade'])`
Then a [VISIBLE] image with the tooltip 'Avatar' exists

Scenario: Step verification Then an image with the tooltip '$tooltipImage' exists
Given I am on a page with the URL 'https://www.w3schools.com/howto/howto_css_image_overlay.asp'
When I change context to element located `By.xpath(.//div[@class='containerfade'])`
Then an image with the tooltip 'Avatar' exists
