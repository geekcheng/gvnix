package org.springframework.roo.support.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.Map.Entry;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Utilities related to Dround tripping XML documents
 * 
 * @author Stefan Schmidt
 * @since 1.1
 * 
 */
public abstract class XmlRoundTripUtils {
	
	private static MessageDigest digest;
	static {
		try {
		  digest = MessageDigest.getInstance("sha-1");
		} catch (NoSuchAlgorithmException e) {
			new IllegalStateException("Could not create hash key for identifier");
		}
	}
	
	/**
	 * Create a base 64 encoded SHA1 hash key for a given XML element. The key is based on the 
	 * element name, the attribute names and their values. Child elements are ignored. 
	 * Attributes named 'z' are not concluded since they contain the hash key itself.
	 * 
	 * @param element The element to create the base 64 encoded hash key for
	 * @return the unique key
	 */
	public static String calculateUniqueKeyFor(Element element) {
		StringBuilder sb = new StringBuilder(); 
		sb.append(element.getTagName());
		NamedNodeMap attributes = element.getAttributes();
		SortedMap<String, String> attrKVStore = Collections.synchronizedSortedMap(new TreeMap<String, String>());
		for (int i = 0; i < attributes.getLength(); i++) {
			Node attr = attributes.item(i);
			if (!"z".equals(attr.getNodeName()) && !attr.getNodeName().startsWith("_")) {
				attrKVStore.put(attr.getNodeName(), attr.getNodeValue());
			}
		}
		for (Entry<String, String> entry: attrKVStore.entrySet()) {
			sb.append(entry.getKey()).append(entry.getValue());
		}
		return base64(sha1(sb.toString().getBytes()));
	}
	
	/**
	 * This method will compare the original document with the proposed document and return 
	 * an adjusted document if necessary. Adjustments are only made if new elements or 
	 * attributes are proposed. Changes to the order of attributes or elements in the 
	 * original document will not result in an adjustment.
	 * 
	 * @param original document as read from the file system
	 * @param proposed document as determined by the JspViewManager
	 * @return the new document if changes are necessary, null if no changes are necessary
	 */
	public static boolean compareDocuments(Document original, Document proposed) {
		boolean originalDocumentAdjusted = false;
		originalDocumentAdjusted = checkNamespaces(original, proposed, originalDocumentAdjusted);
		originalDocumentAdjusted = addOrUpdateElements(original.getDocumentElement(), proposed.getDocumentElement(), originalDocumentAdjusted);
		originalDocumentAdjusted = removeElements(original.getDocumentElement(), proposed.getDocumentElement(), originalDocumentAdjusted);
		return originalDocumentAdjusted;
	}
	
	/**
	 * Compare necessary namespace declarations between original and proposed document, if 
	 * namespaces in the original are missing compared to the proposed, we add them to the 
	 * original.
	 * 
	 * @param original document as read from the file system
	 * @param proposed document as determined by the JspViewManager
	 * @return the new document if changes are necessary, null if no changes are necessary
	 */
	private static boolean checkNamespaces(Document original, Document proposed, boolean originalDocumentChanged) {
		NamedNodeMap nsNodes = proposed.getDocumentElement().getAttributes();
		for (int i = 0; i < nsNodes.getLength(); i++) {
			if (0 == original.getDocumentElement().getAttribute(nsNodes.item(i).getNodeName()).length()) {
				original.getDocumentElement().setAttribute(nsNodes.item(i).getNodeName(), nsNodes.item(i).getNodeValue());
				originalDocumentChanged = true;
			}
		}
		return originalDocumentChanged;
	}
	
	private static boolean addOrUpdateElements(Element original, Element proposed, boolean originalDocumentChanged) {
		NodeList proposedChildren = proposed.getChildNodes();
		for (int i = 0; i < proposedChildren.getLength(); i++) { //check proposed elements and compare to originals to find out if we need to add or replace elements
			Node node = proposedChildren.item(i);
			if (node.getNodeType() == Node.ELEMENT_NODE) {
				Element proposedElement = (Element) node;
				String proposedId = proposedElement.getAttribute("id");
				if (proposedId.length() != 0) { //only proposed elements with an id will be considered
					Element originalElement = XmlUtils.findFirstElement("//*[@id='" + proposedId + "']", original);				
					if (null == originalElement) { //insert proposed element given the original document has no element with a matching id
						Element placeHolder = XmlUtils.findFirstElementByName("util:placeholder", original);
						if (placeHolder != null) { //insert right before place holder if we can find it
							placeHolder.getParentNode().insertBefore(original.getOwnerDocument().importNode(proposedElement, false), placeHolder);
						} else { //find the best place to insert the element
							if (proposed.getAttribute("id").length() != 0) { //try to find the id of the proposed element's parent id in the original document 
								Element originalParent = XmlUtils.findFirstElement("//*[@id='" + proposed.getAttribute("id") + "']", original);
								if (originalParent != null) { //found parent with the same id, so we can just add it as new child
									originalParent.appendChild(original.getOwnerDocument().importNode(proposedElement, false));
								} else { //no parent found so we add it as a child of the root element (last resort)
									original.appendChild(original.getOwnerDocument().importNode(proposedElement, false));
								}
							} else { //no parent found so we add it as a child of the root element (last resort)
								original.appendChild(original.getOwnerDocument().importNode(proposedElement, false));
							}
						}
						originalDocumentChanged = true;
					} else { //we found an element in the original document with a matching id	
						String originalElementHashCode = originalElement.getAttribute("z");
						if (originalElementHashCode.length() > 0) { //only act if a hash code exists
							if ("?".equals(originalElementHashCode) || originalElementHashCode.equals(calculateUniqueKeyFor(originalElement))) { //only act if hash codes match (no user changes in the element) or the user requests for the hash code to be regenerated
								if (!equalElements(originalElement, proposedElement)) { //check if the elements have equal contents
									originalElement.getParentNode().replaceChild(original.getOwnerDocument().importNode(proposedElement, false), originalElement); //replace the original with the proposed element
									originalDocumentChanged = true;
								}
								if ("?".equals(originalElementHashCode)) { //replace z if the user sets its value to '?' as an indication that roo should take over the management of this element again
									originalElement.setAttribute("z", calculateUniqueKeyFor(proposedElement));
									originalDocumentChanged = true;
								} 
							} else { //if hash codes don't match we will mark the element as z="user-managed"
								if (!originalElementHashCode.equals("user-managed")) {
									originalElement.setAttribute("z", "user-managed"); //mark the element as 'user-managed' if the hash codes don't match any more
									originalDocumentChanged = true;
								}
							}	
						}
					}
				}
				originalDocumentChanged = addOrUpdateElements(original, proposedElement, originalDocumentChanged); //walk through the document tree recursively
			}
		}
		return originalDocumentChanged;
	}
	
	private static boolean removeElements(Element original, Element proposed, boolean originalDocumentChanged) {
		NodeList originalChildren = original.getChildNodes();
		for (int i = 0; i < originalChildren.getLength(); i++) { //check original elements and compare to proposed to find out if we need to remove elements
			Node node = originalChildren.item(i);
			if (node.getNodeType() == Node.ELEMENT_NODE) {
				Element originalElement = (Element) node;
				String originalId = originalElement.getAttribute("id");
				if (originalId.length() != 0) { //only proposed elements with an id will be considered
					Element proposedElement = XmlUtils.findFirstElement("//*[@id='" + originalId + "']", proposed);		
					if (null == proposedElement && (originalElement.getAttribute("z").equals(calculateUniqueKeyFor(originalElement)) || originalElement.getAttribute("z").equals("?"))) { //remove original element given the proposed document has no element with a matching id
						originalElement.getParentNode().removeChild(originalElement);
						originalDocumentChanged = true;
					}
				}
				originalDocumentChanged = removeElements(originalElement, proposed, originalDocumentChanged); //walk through the document tree recursively
			}
		}
		return originalDocumentChanged;
	}
	
	private static boolean equalElements(Element a, Element b) {
		if (!a.getTagName().equals(b.getTagName())) { 
			return false;
		}
		NamedNodeMap attributes = a.getAttributes();
		int customAttributeCounter = 0;
		for (int i = 0; i < attributes.getLength(); i++) {
			Node node = attributes.item(i);
			if (!node.getNodeName().startsWith("_")) {
				if (!node.getNodeName().equals("z") && (b.getAttribute(node.getNodeName()).length() == 0 || !b.getAttribute(node.getNodeName()).equals(node.getNodeValue()))) {
					return false;
				}
			} else {
				customAttributeCounter++;
			}
		}
		if (a.getAttributes().getLength() - customAttributeCounter != b.getAttributes().getLength()) {
			return false;
		}
		return true;
	}
	
	/**
	 * Creates a sha-1 hash value for the given data byte array.
	 * 
	 * @param data to hash
	 * @return byte[] hash of the input data
	 */
	private static byte[] sha1(byte[] data) {
		Assert.notNull(digest, "Could not create hash key for identifier");
		return digest.digest(data);
	}
	
	private static String base64(byte[] data) {
		return Base64.encodeBytes(data);
	}
}
