package de.rwth.i9.palm.datasetcollect.service;

public class MicrosoftAcademicSearchPublicationCollection extends PublicationCollection
{

	/**
	 * Old API url
	 * http://academic.research.microsoft.com/json.svc/search?AppId=e028e2a5-
	 * 972d-4ba5-a4c0-2f25860b1203&AuthorQuery=mohamed%20amine%20chatti&
	 * ResultObjects=Publication&PublicationContent=AllInfo&StartIdx=0&EndIdx=99
	 * 
	 * New API == get author id
	 * https://api.datamarket.azure.com/Data.ashx/MRC/MicrosoftAcademic/v2/
	 * Author?$filter=Name+eq+%27mohamed%20amine%20chatti%27&$format=json == get
	 * publication id list
	 * https://api.datamarket.azure.com/Data.ashx/MRC/MicrosoftAcademic/v2/
	 * Paper_Author?$filter=AuthorID%20+eq+220509&$format=json&$skip=0 == get
	 * publication detail
	 * 
	 */
}
